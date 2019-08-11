package backend.flow.spark.bahir

import akka.actor.{Actor, ActorRef}
import backend.flow.spark.bahir.SparkStreamingController.Subscribe

class SparkStreamingController extends Actor {

  var subscribers: List[(String, ActorRef)] = List()

  override def receive: Receive = {

    case Subscribe(id, sparkJob) => {
      println(s"New spark job subscribed ($id, $sparkJob)")
      subscribers = subscribers.::((id, sparkJob))
    }

    case msg: String => {
      if (subscribers.isEmpty) println("Empty subscribers")
      else {
        println(s"Message received at SparkControlled and sent to subscribers: ${subscribers.mkString(",")}")
        subscribers.foreach(_._2 ! msg)
      }

    }
  }

}
object SparkStreamingController {
  case class Subscribe(id: String, sparkJob: ActorRef)
}
