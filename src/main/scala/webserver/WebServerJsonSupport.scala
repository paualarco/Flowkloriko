package webserver

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import backend.Master
import backend.flow.Flowkloriko
import spray.json.DefaultJsonProtocol

trait WebServerJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val source = jsonFormat3(Flowkloriko.SourceSchema)
  implicit val processor = jsonFormat4(Flowkloriko.ProcessorSchema)
  implicit val flow = jsonFormat3(Flowkloriko.FlowSchema)
  implicit val sink = jsonFormat3(Flowkloriko.SinkSchema)
  implicit val createPipeline = jsonFormat1(Master.CreateFlow)
  implicit val modifyPipeline = jsonFormat1(Master.ModifyFlow)
  implicit val startPipeline = jsonFormat1(Master.StartFlow)
  implicit val stopPipeline = jsonFormat1(Master.StopFlow)
  implicit val checkPipeline = jsonFormat1(Master.CheckFlow)
}
