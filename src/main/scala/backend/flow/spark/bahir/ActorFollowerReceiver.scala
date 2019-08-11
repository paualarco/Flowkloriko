package backend.flow.spark.bahir

class ActorFollowerReceiver(actorToFollow: String) extends ActorReceiver {

    println("Akka follower created")

  lazy private val sparkControllerRef = context.actorSelection(actorToFollow)

  override def preStart(): Unit = sparkControllerRef ! SparkStreamingController.Subscribe("actorFollower", self)

    override def receive: PartialFunction[Any, Unit] = {
      case msg: String => {
        println(s"Sending received message from akkaFollower to supervisor to akkaFollower: $msg")
        context.parent ! AkkaReceiver.CheckHiveTable("inventedId", msg)
      }
    }

  override def postStop(): Unit = {}

}
object ActorFollowerReceiver {
}
