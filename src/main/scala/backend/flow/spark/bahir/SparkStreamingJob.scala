package backend.flow.spark.bahir

import akka.actor.Props
import org.apache.spark._
import org.apache.spark.streaming._

class SparkStreamingJob(actorToFollow: String, props: Props) {
  //flowkloriko: ActorRef) {




  val conf = new SparkConf().setAppName("Spark-streaming-akka").setMaster("local[2]")
  val ssc = new StreamingContext(conf, Seconds(2))

  val msgs = ssc.receiverStream[AkkaReceiver.AkkaSparkProtocol](new AkkaReceiver(props))

  //val msgs = AkkaUtils.createStream[String](ssc, props, "akkaReceiver")

  //msgs.flatMap(_.split("\\s+")).map(x => (x, 1)).reduceByKey(_ + _).print()

  object AkkaInterpreter {
    lazy implicit val system = AkkaReceiver.defaultActorSystemCreator.apply()
    lazy private val sparkControllerRef = system.actorSelection(actorToFollow)

    def interpret(): Unit = {
      println("Interpret from akka interpreter")
      sparkControllerRef ! AkkaReceiver.CheckedHiveTable("akkaInterpreter")
    }
  }


  msgs.map{
    akkaOrder =>
    akkaOrder match {
      case check: AkkaReceiver.CheckHiveTable => {
        println("Check Hive Table")
        check
      }
    }
  }.print()


  /*
 val ss = SparkSession.builder().appName("Spark-with-akka-test").master("local").getOrCreate()
   val stream = ss.readStream.textFile("src/test/resources").writeStream.outputMode("append").format("console").start()

 stream.awaitTermination(100000)
*/

  ssc.start()
  ssc.awaitTerminationOrTimeout(60)

}
