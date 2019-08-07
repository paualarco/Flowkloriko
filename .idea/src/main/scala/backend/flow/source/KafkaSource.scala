package backend.flow.source

import akka.actor.{Actor, Props}
import backend.flow.source.KafkaSource.{CheckKafkaBroker, CheckKafkaTopic, ResultKafkaBroker, ResultKafkaTopic}
import spray.json.{JsArray, JsValue}

class KafkaSource extends Actor {
  def receive: Receive = {
    case SourceMonitor.CheckSource => {
      println("CheckSource at KafkaSource")
    }
    case CheckKafkaBroker(broker: String) => {
      println("Kafka broker is alive")
      sender() ! ResultKafkaBroker(true)
    }
    case CheckKafkaTopic(topic: String) => {
      println(s"Kafka topic: $topic exists")
      sender() ! ResultKafkaTopic(true)
    }


  }
}
object KafkaSource {

  val props = Props(new KafkaSource)
  case class CheckKafkaBroker(broker: String)
  case class CheckKafkaTopic(topic: String)

  case class ResultKafkaBroker(exists: Boolean)
  case class ResultKafkaTopic(exists: Boolean)



}
