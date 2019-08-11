package backend

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import backend.Master.CreateFlow
import backend.flow.Flowkloriko.FlowSchema

class Master extends Actor with ActorLogging {

  var pipelineRegistry = Map[String, (ActorRef, Master.Status.Status)]()

  def receive = {

    case CreateFlow(pipeline) => {
      if(pipelineRegistry.keys.toList.contains(pipeline.id)) println(s"The pipeline ${pipeline.id} already exists")
      else {
        println(s"PipelineSchema with id:${pipeline.id} registered at pipelineRegistry")

       // pipelineRegistry = pipelineRegistry.updated(pipeline.id, (pipelineController, Master.Status.Started))
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

  trait Action
  case class CreateFlow(flowlineToBeCreated: FlowSchema) extends Action
  case class ModifyFlow(flowToBeModified: FlowSchema) extends Action
  case class StartFlow(flowToBeStarted: String) extends Action
  case class StopFlow(flowToBeStopped: String) extends Action
  case class CheckFlow(flowToBeChecked: String) extends Action
  case class DeleteFlow(flowToBeDeleted: String) extends Action




}
