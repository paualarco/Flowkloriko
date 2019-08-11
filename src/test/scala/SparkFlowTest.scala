import java.sql.DriverManager

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import backend.flow.spark.SparkController
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class SparkFlowTest extends TestKit(ActorSystem("SparkInterceptorTest"))
with ImplicitSender
with WordSpecLike
with Matchers
with BeforeAndAfterAll {



  val sparkController = system.actorOf(Props(new SparkController), "SparkStreamingController")
  println(s"SparkStreamingController with url: ${sparkController.path.toString} ")
  //new SparkStreamingJob(Props(new ActorFollowerReceiver( "akka.tcp://SparkFollowTest@192.168.1.23:2552/user/SparkStreamingController")))

  //val remoteSystem = new SampleRemoteActor("akka.tcp://SparkFollowTest@192.168.1.23:59218/user/SparkStreamingController", system)
  Thread.sleep(3000)
  for (i <- 1 to 30){
    Thread.sleep(50)
    sparkController ! SparkController.StartSparkJob(i.toString)
  }
  Thread.sleep(30000)

}