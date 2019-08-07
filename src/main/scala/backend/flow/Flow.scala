package backend.flow

object Flow {
  trait DataPointSchema {
    val id: String
    val dataPoint: String
    val configs: List[(String)]
  }
  case class SourceSchema(id: String, dataPoint: String, configs: List[(String)], pipeTo: List[String]) extends DataPointSchema
  case class ProcessorSchema(id: String, processor: String, pipeTo: List[String], pipedFrom: List[String])
  case class SinkSchema(id: String, dataPoint: String, configs: List[(String)], pipedFrom: List[String]) extends DataPointSchema
  case class PipelineSchema(id: String, sources: List[SourceSchema], processors: List[ProcessorSchema], sinks: List[SinkSchema])
}
