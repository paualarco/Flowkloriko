package webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{as, complete, concat, entity, get, pathSingleSlash, post}
import akka.stream.ActorMaterializer
import backend.Master
import backend.flow.Flowkloriko
import backend.flow.Flowkloriko.Start

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
            entity(as[Master.CreateFlow]) { createPipeline =>
              master ! createPipeline
              complete(s"Received $createPipeline")
            }
            ,

            entity(as[Master.ModifyFlow]) { modifyPipeline =>
              complete(s"Received $modifyPipeline")
            }
            ,
            entity(as[Master.StartFlow]) { startPipeline =>
              val flowkloriko = system.actorOf(Flowkloriko.props)
              flowkloriko ! Start()
              complete(s"Received $startPipeline")
            }
            ,
            entity(as[Master.StopFlow]) { stopPipeline =>
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
