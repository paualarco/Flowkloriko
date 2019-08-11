import backend.pipeline
import akka.actor.{ActorSystem, Props}
import backend.flow.spark.bahir.{ActorFollowerReceiver, SparkStreamingController, SparkStreamingJob}
import com.typesafe.config.ConfigFactory

object Main {
  def main(args: Array[String]): Unit = {


    val conf = ConfigFactory.parseString(
      s"""akka {
         |  actor {
         |    provider = remote
         |  }
         |  remote {
         |    enabled-transports = ["akka.remote.netty.tcp"]
         |    netty.tcp {
         |      hostname = "127.0.0.1"
         |      port = 2552
         |    }
         |  }
         |}""".stripMargin)

    val system = ActorSystem("Test", conf)

    val sparkController = system.actorOf(Props(new SparkStreamingController), "SparkStreamingController")
    println(s"SparkStreamingController with url: ${sparkController.path.toString} ")
    val tcp = "akka.tcp://Test@127.0.0.1:2552/user/SparkStreamingController"
    println(s"TCP url $tcp ")
    new SparkStreamingJob(tcp, Props(new ActorFollowerReceiver(tcp)))
    //val remoteSystem = new SampleRemoteActor("akka.tcp://SparkFollowTest@192.168.1.23:59218/user/SparkStreamingController", system)

    for (i <- 1 to 30) {
      Thread.sleep(3000)
      sparkController ! s"Message number $i"
    }

  }
}