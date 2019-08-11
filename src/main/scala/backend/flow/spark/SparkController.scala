package backend.flow.spark

import akka.actor.Actor
import backend.flow.Flowkloriko.SimpleSparkJobSchema
import backend.flow.source.SourceMonitor.CheckSource
import backend.flow.spark.SparkController.{JobDone, SparkJobsStatus, StartSparkJob}

class SparkController extends Actor {

  val sparkInterface = new SparkInterceptor(self)

  override def receive: Receive = {

    case StartSparkJob(id) => {
      sparkInterface.runJob(id, SimpleSparkJobSchema(id))
    }

    case jobDone: JobDone => {
      println(s"Received $jobDone at SparkController")
    }

    case sparkJobs: SparkJobsStatus => {
      println(s"SparkJobStatus received, there are ${sparkInterface.ss.streams.active.length}")
    }
  }
}

object SparkController {

  case class CheckSource(id: String, tableName: String)

  case class CheckedSource(id: String)

  case class JobDone(id: String)

  case class StartSparkJob(id: String)

  case class SparkJobsStatus()
}
