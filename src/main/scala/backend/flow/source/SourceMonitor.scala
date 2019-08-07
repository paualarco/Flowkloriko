package backend.flow.source

import akka.actor.{Actor, ActorRef}
import backend.flow.source.SourceMonitor.CheckSource
import spray.json.JsValue

class SourceMonitor extends Actor {

  val sources: Map[Sources.Value, ActorRef] = Map(Sources.Kafka -> context.actorOf(KafkaSource.props))

  def receive: Receive = {
    case CheckSource(source) => {
      println(s"CheckSource $source at SourceMonitor")
      sources(source) ! CheckSource
    }
  }
}

object SourceMonitor {

  case class CheckSource(source: Sources.Value)



}


