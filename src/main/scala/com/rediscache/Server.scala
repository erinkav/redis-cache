package com.rediscache

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

object QuickstartServer extends App with Routes {

  val config = ConfigFactory.load()
  val appConfig = config.getConfig("application")
  val redisConfig = appConfig.getConfig("redis")

  implicit val system: ActorSystem = ActorSystem("redisCacheHttpServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Initialize actors and start server
  val redisActor: ActorRef = system.actorOf(RedisActor.props, "redisActor")
  val localCacheActor: ActorRef = system.actorOf(LocalCacheActor.props, "localCacheActor")
  lazy val routes: Route = route

  // Host and port are configurable in the application configs
  val host: String = appConfig.getString("host")
  val port: Int = appConfig.getInt("port")
  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, host, port)

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
