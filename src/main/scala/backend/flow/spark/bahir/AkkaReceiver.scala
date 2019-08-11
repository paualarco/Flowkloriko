package backend.flow.spark.bahir

import akka.actor.{Actor, ActorSystem, Props}
import backend.flow.spark.bahir.AkkaReceiver.AkkaSparkProtocol
import com.typesafe.config.ConfigFactory
import org.apache.spark.TaskContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver

class AkkaReceiver(props: Props) extends Receiver[AkkaSparkProtocol](StorageLevel.MEMORY_AND_DISK_SER_2) {

  //Without this props (unique + remote) the new actor system would be unable to comunicate with the main one (dead letters error)
  lazy implicit val system = AkkaReceiver.defaultActorSystemCreator.apply()
  lazy val actorSupervisor = system.actorOf(Props(new Supervisor()), "supervisor")

  override def onStart(): Unit = {
    println("Subscribing to sparkController from AkkaReceiver ")
    actorSupervisor
  }

  override def onStop(): Unit = {
    println("Stopping akka receiver")
    system.terminate()
  }

  class Supervisor extends Actor {
    val akkaFollowerReceiver = context.actorOf(props, "akkaFollowerReceiver")
    override def receive: PartialFunction[Any, Unit] = {
      case msg: AkkaSparkProtocol =>
        println("received single")
        store(msg)
    }
  }


}
object AkkaReceiver {

  abstract class ActorReceiver extends Actor {
    def store(item: String) {
      context.parent ! SingleItemData(item)
    }
  }
  private case class SingleItemData(item: String)

  val defaultActorSystemCreator: () => ActorSystem = () => {
    val uniqueSystemName = s"streaming-actor-system-${TaskContext.get().taskAttemptId()}"
    val akkaConf = ConfigFactory.parseString(
      s"""akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
         |akka.remote.netty.tcp.port = "0"
         |""".stripMargin)
    ActorSystem(uniqueSystemName, akkaConf)
  }

  sealed trait AkkaSparkProtocol { val id: String}
  case class TestProcessor(id: String) extends AkkaSparkProtocol
  case class CheckHiveTable(id: String, tableName: String) extends AkkaSparkProtocol
  case class CheckedHiveTable(id: String) extends AkkaSparkProtocol

}
