package backend.flow.spark.bahir

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag
import akka.actor._
import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.spark.TaskContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver


object ActorReceiver {

  /**
    * A OneForOneStrategy supervisor strategy with `maxNrOfRetries = 10` and
    * `withinTimeRange = 15 millis`. For RuntimeException, it will restart the ActorReceiver; for
    * others, it just escalates the failure to the supervisor of the supervisor.
    */
  val defaultSupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange =
    15 millis) {
    case _: RuntimeException => Restart
    case _: Exception => Escalate
  }


  val defaultActorSystemCreator: () => ActorSystem = () => {
    val uniqueSystemName = s"streaming-actor-system-${TaskContext.get().taskAttemptId()}"
    val akkaConf = ConfigFactory.parseString(
      s"""akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
         |akka.remote.netty.tcp.port = "0"
         |""".stripMargin)
    ActorSystem(uniqueSystemName, akkaConf)
  }
}


abstract class ActorReceiver extends Actor {

  /** Store an iterator of received data as a data block into Spark's memory. */
  def store[T](iter: Iterator[T]) {
    context.parent ! IteratorData(iter)
  }

  /**
    * Store the bytes of received data as a data block into Spark's memory. Note
    * that the data in the ByteBuffer must be serialized using the same serializer
    * that Spark is configured to use.
    */
  def store(bytes: ByteBuffer) {
    context.parent ! ByteBufferData(bytes)
  }

  /**
    * Store a single item of received data to Spark's memory asynchronously.
    * These single items will be aggregated together into data blocks before
    * being pushed into Spark's memory.
    */
  def store[T](item: T) {
    context.parent ! SingleItemData(item)
  }

  /**
    * Store a single item of received data to Spark's memory and returns a `Future`.
    * The `Future` will be completed when the operator finishes, or with an
    * `akka.pattern.AskTimeoutException` after the given timeout has expired.
    * These single items will be aggregated together into data blocks before
    * being pushed into Spark's memory.
    *
    * This method allows the user to control the flow speed using `Future`
    */
  def store[T](item: T, timeout: Timeout): Future[Unit] = {
    context.parent.ask(AskStoreSingleItemData(item))(timeout).map(_ => ())(context.dispatcher)
  }
}


abstract class JavaActorReceiver extends UntypedActor {

  /** Store an iterator of received data as a data block into Spark's memory. */
  def store[T](iter: Iterator[T]) {
    context.parent ! IteratorData(iter)
  }

  /**
    * Store the bytes of received data as a data block into Spark's memory. Note
    * that the data in the ByteBuffer must be serialized using the same serializer
    * that Spark is configured to use.
    */
  def store(bytes: ByteBuffer) {
    context.parent ! ByteBufferData(bytes)
  }

  /**
    * Store a single item of received data to Spark's memory.
    * These single items will be aggregated together into data blocks before
    * being pushed into Spark's memory.
    */
  def store[T](item: T) {
    context.parent ! SingleItemData(item)
  }

  /**
    * Store a single item of received data to Spark's memory and returns a `Future`.
    * The `Future` will be completed when the operator finishes, or with an
    * `akka.pattern.AskTimeoutException` after the given timeout has expired.
    * These single items will be aggregated together into data blocks before
    * being pushed into Spark's memory.
    *
    * This method allows the user to control the flow speed using `Future`
    */
  def store[T](item: T, timeout: Timeout): Future[Unit] = {
    context.parent.ask(AskStoreSingleItemData(item))(timeout).map(_ => ())(context.dispatcher)
  }
}


case class Statistics(numberOfMsgs: Int,
                      numberOfWorkers: Int,
                      numberOfHiccups: Int,
                      otherInfo: String)

/** Case class to receive data sent by child actors */
private sealed trait ActorReceiverData
private case class SingleItemData[T](item: T) extends ActorReceiverData
private case class AskStoreSingleItemData[T](item: T) extends ActorReceiverData
private case class IteratorData[T](iterator: Iterator[T]) extends ActorReceiverData
private case class ByteBufferData(bytes: ByteBuffer) extends ActorReceiverData
private object Ack extends ActorReceiverData

/**
  * Provides Actors as receivers for receiving stream.
  *
  * As Actors can also be used to receive data from almost any stream source.
  * A nice set of abstraction(s) for actors as receivers is already provided for
  * a few general cases. It is thus exposed as an API where user may come with
  * their own Actor to run as receiver for Spark Streaming input source.
  *
  * This starts a supervisor actor which starts workers and also provides
  * [http://doc.akka.io/docs/akka/snapshot/scala/fault-tolerance.html fault-tolerance].
  *
  * Here's a way to start more supervisor/workers as its children.
  *
  * @example {{{
  *  context.parent ! Props(new Supervisor)
  * }}} OR {{{
  *  context.parent ! Props(new Worker, "Worker")
  * }}}
  */
private class ActorReceiverSupervisor[T: ClassTag](
                                                          actorSystemCreator: () => ActorSystem,
                                                          props: Props,
                                                          name: String,
                                                          storageLevel: StorageLevel,
                                                          receiverSupervisorStrategy: SupervisorStrategy
                                                        ) extends Receiver[T](storageLevel) {

  private lazy val actorSystem = actorSystemCreator()
  protected lazy val actorSupervisor = actorSystem.actorOf(Props(new Supervisor),
    "Supervisor" + streamId)

  class Supervisor extends Actor {

    override val supervisorStrategy = receiverSupervisorStrategy
    private val worker = context.actorOf(props, name)
    println("Started receiver worker at:" + worker.path)

    private val n: AtomicInteger = new AtomicInteger(0)
    private val hiccups: AtomicInteger = new AtomicInteger(0)

    override def receive: PartialFunction[Any, Unit] = {

      case IteratorData(iterator) =>
        println("received iterator")
        store(iterator.asInstanceOf[Iterator[T]])

      case SingleItemData(msg) =>
        println("received single")
        store(msg.asInstanceOf[T])
        n.incrementAndGet

      case AskStoreSingleItemData(msg) =>
        println("received single sync")
        store(msg.asInstanceOf[T])
        n.incrementAndGet
        sender() ! Ack

      case ByteBufferData(bytes) =>
        println("received bytes")
        store(bytes)

      case props: Props =>
        val worker = context.actorOf(props)
        println("Started receiver worker at:" + worker.path)
        sender ! worker

      case (props: Props, name: String) =>
        val worker = context.actorOf(props, name)
        println("Started receiver worker at:" + worker.path)
        sender ! worker

      case _: PossiblyHarmful => hiccups.incrementAndGet()

      case _: Statistics =>
        val workers = context.children
        sender ! Statistics(n.get, workers.size, hiccups.get, workers.mkString("\n"))

    }
  }

  def onStart(): Unit = {
    actorSupervisor
    println("Supervision tree for receivers initialized at:" + actorSupervisor.path)
  }

  def onStop(): Unit = {
    actorSupervisor ! PoisonPill
    Await.ready(actorSystem.terminate(), Duration.Inf)
  }
}

import scala.reflect.ClassTag
import akka.actor.{ActorSystem, Props, SupervisorStrategy}
import org.apache.spark.api.java.function.{Function0 => JFunction0}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.api.java.{JavaReceiverInputDStream, JavaStreamingContext}
import org.apache.spark.streaming.dstream.ReceiverInputDStream

object AkkaUtils {


  def createStream[T: ClassTag](
                                 ssc: StreamingContext,
                                 propsForActor: Props,
                                 actorName: String,
                                 storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2,
                                 actorSystemCreator: () => ActorSystem = ActorReceiver.defaultActorSystemCreator,
                                 supervisorStrategy: SupervisorStrategy = ActorReceiver.defaultSupervisorStrategy
                               ): ReceiverInputDStream[T] = {
    ssc.receiverStream(new ActorReceiverSupervisor[T](
      actorSystemCreator,
      propsForActor,
      actorName,
      storageLevel,
      supervisorStrategy))
  }

}