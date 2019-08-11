package backend.flow

import akka.actor.{Actor, Props}
import backend.Master.StartFlow
import backend.flow.Flowkloriko.{FinishedJob, Start}
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class Flowkloriko extends Actor {

  implicit val ec = ExecutionContext.global
  //Future(new SparkStreamingJob(context.self))
  def receive: Receive = {
    case Start() => {
      println("StartSparkFlow")
    }
    case FinishedJob() =>{
      def print {println("Job finished from spark")}
      for ( i <- 0 to 10 ) { print}
    }
  }
}
  object Flowkloriko{

    val props = Props(new Flowkloriko)
  trait DataPointSchema {
    val id: String
    val dataPoint: String
    val configs: List[(String)]
  }
  case class SourceSchema(id: String, dataPoint: String, configs: List[(String)]) extends DataPointSchema
  case class ProcessorSchema(id: String, processor: JsValue, pipedFrom: String, pipeTo: String)
  case class SinkSchema(id: String, dataPoint: String, configs: List[(String)]) extends DataPointSchema
  case class FlowSchema(id: String, flow: String, processors: List[ProcessorSchema])
  case class FinishedJob()
  case class Start()

    case class SimpleSparkJobSchema(id: String)
    case class SparkJobSchema(id: String, source: SourceSchema, processor: ProcessorSchema, sink: SinkSchema)
}
