package backend

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import backend.pipeline.Flow

class Master extends Actor with ActorLogging {

  var pipelineRegistry = Map[String, (ActorRef, Master.Status.Status)]()

  def receive = {

    case Flow.CreateFlow(pipeline) => {
      if(pipelineRegistry.keys.toList.contains(pipeline.id)) println(s"The pipeline ${pipeline.id} already exists")
      else {
        println(s"PipelineSchema with id:${pipeline.id} registered at pipelineRegistry")
        val pipelineController = context.actorOf(Flow.props)
        pipelineRegistry = pipelineRegistry.updated(pipeline.id, (pipelineController, Master.Status.Started))
        pipelineController ! Flow.CreateFlow(pipeline)
      }
    }
  }
}

object Master {
  val props = Props(new Master)
  object Status extends Enumeration {
    val Started, Running, Finished, Failed = Value
    type Status = Value
  }


}
