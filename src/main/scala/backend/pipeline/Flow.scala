package backend.pipeline

import akka.actor.{Actor, ActorLogging, Props}
import backend.flow.Flow.{DataPointSchema, PipelineSchema, SourceSchema}
import backend.pipeline.Flow.{CheckFlow, CreateFlow, StartFlow, StopFlow}

class Flow extends Actor with ActorLogging {

  var pipelineDataPoints = Map[String, DataPointSchema]()
  var sourcesNotCheckedYet = Set[String]()

  val updateDataPoint: DataPointSchema => Unit = (dataPoint => pipelineDataPoints = pipelineDataPoints.updated(dataPoint.id, dataPoint))
  def receive: Receive = {
    case CreateFlow(pipeline) => {
      println(s"Created pipeline with schema: ${pipeline}")
      pipeline.sources.map(updateDataPoint)
      pipeline.sinks.map(updateDataPoint)
      println(s"PipelineDataPoins: ${pipelineDataPoints.mkString(", ")}")
      context.become(running)
    }
  }

  def running: Receive = {
    case StopFlow(id: String) =>
      println(s"Stopped pipeline ")
    case StartFlow(id: String) => {
    }
    case CheckFlow(pipelineToBeChecked) => {}

  }
}

object Flow {
  val props = Props(new Flow)
  trait Action
  case class CreateFlow(pipelineToBeCreated: PipelineSchema) extends Action
  case class ModifyFlow(pipelineToBeModified: PipelineSchema) extends Action
  case class StartFlow(id: String) extends Action
  case class StopFlow(id: String) extends Action
  case class CheckFlow(id: String) extends Action
}
