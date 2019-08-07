package webserver

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import backend.flow.Flow
import backend.pipeline.Flow
import spray.json.DefaultJsonProtocol

trait WebServerJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val source = jsonFormat4(Flow.SourceSchema)
  implicit val processor = jsonFormat4(Flow.ProcessorSchema)
  implicit val sink = jsonFormat4(Flow.SinkSchema)
  implicit val pipeline = jsonFormat4(Flow.PipelineSchema)
  implicit val createPipeline = jsonFormat1(Flow.CreatePipeline)
  implicit val modifyPipeline = jsonFormat1(Flow.ModifyPipeline)
  implicit val startPipeline = jsonFormat1(Flow.StartPipeline)
  implicit val stopPipeline = jsonFormat1(Flow.StopPipeline)
  implicit val checkPipeline = jsonFormat1(Flow.CheckPipeline)
}
