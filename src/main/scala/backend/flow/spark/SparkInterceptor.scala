package backend.flow.spark

import akka.actor.ActorRef
import backend.flow.Flowkloriko
import backend.flow.Flowkloriko.SimpleSparkJobSchema
import org.apache.spark.sql.SparkSession

class SparkInterceptor(sparkController: ActorRef) {


    val ss = SparkSession.builder().appName("Spark-with-akka-test").master("local").getOrCreate()

    val rdd = ss.sparkContext.textFile("src/test/resources/stackoverflow.csv")
    val nWords = rdd.map( line => line.split(" ")).map(words => words.length).reduce(_ + _)

    println(s"The file stackoverflow.csv has $nWords words")
    println(s"The first line of the RDD is: ${rdd.first()}")


    def runJob(id: String, sparkJobSchem: SimpleSparkJobSchema): Unit = {
      val rdd = ss.sparkContext.textFile("src/test/resources/stackoverflow.csv")
      rdd.map( line => line.split(" ")).map(words => words.length).reduce(_ + _)
      def count(): String = ss.sparkContext.parallelize(0 to 500000, 25).count.toString
      //println(s"The file stackoverflow.csv has $nWords words")
      //println(s"The first line of the RDD is: ${rdd.first()}")
      sparkController ! SparkController.JobDone(id)
      sparkController ! SparkController.SparkJobsStatus()
    }



}
