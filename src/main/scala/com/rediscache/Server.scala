package com.rediscache

//#quick-start-server
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

//#main-class
object QuickstartServer extends App with Routes {

  // set up ActorSystem and other dependencies here
  //#main-class
  //#server-bootstrapping
  val config = ConfigFactory.load()
  println(config)
  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  //#server-bootstrapping
  val redisActor: ActorRef = system.actorOf(RedisActor.props, "redisActor")
  //#main-class
  // from the UserRoutes trait
  lazy val routes: Route = route
  val host = "localhost"
  val port = 8080
  //  val host: String = config.getString("application/host")
  //  val port: Int = config.getInt("application/port")
  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, host, port)
  val redis = RedisInterface

  println("redis" + redis)
  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
