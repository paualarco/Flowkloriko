package webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{as, complete, concat, entity, get, pathSingleSlash, post}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.javadsl.Consumer
import akka.stream.ActorMaterializer
import backend.Master
import backend.pipeline.Flow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.io.StdIn

object WebServer extends App with WebServerJsonSupport {
  implicit val system = ActorSystem("RestService")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val master = system.actorOf(Master.props)
  val route = {
    pathSingleSlash {
      concat(
        get { complete("Hello get!") }
        ,
        post {
          concat(
            entity(as[Flow.CreateFlow]) { createPipeline =>
              master ! createPipeline
              complete(s"Received $createPipeline")
            }
            ,
            entity(as[Flow.ModifyFlow]) { modifyPipeline =>
              complete(s"Received $modifyPipeline")
            }
            ,
            entity(as[Flow.StartFlow]) { startPipeline =>
              val config = system.settings.config.getConfig("akka.kafka.consumer")
              val consumerSettings =
                ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
                  .withBootstrapServers("")
                  .withGroupId("group1")
                  .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
              val subscription = Subscriptions.topics("topic")
              val consumer = Consumer.plainSource(consumerSettings, subscription)
              complete(s"Received $startPipeline")
            }
            ,
            entity(as[Flow.StopFlow]) { stopPipeline =>
              complete(s"Received $stopPipeline")
            }

          )
        }
      )
    }
  }


  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
