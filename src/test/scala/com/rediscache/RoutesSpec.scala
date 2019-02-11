package com.rediscache

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.redis.RedisClient
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scalatest.concurrent.ScalaFutures

class RoutesSpec extends WordSpec with BeforeAndAfterAll with Matchers with ScalaFutures with ScalatestRouteTest
  with Routes {
  override val redisActor: ActorRef =
    system.actorOf(RedisActor.props, "cacheActor")
  override val localCacheActor: ActorRef = system.actorOf(LocalCacheActor.props, "localCacheActor")
  lazy val routes: Route = route
  val config: Config = ConfigFactory.load()
  val redisConfig: Config = config.getConfig("application").getConfig("redis")
  var redisClient: RedisClient = new RedisInterface(redisConfig).client

  override def beforeAll() {
    redisClient.set("test1", "{}")
    redisClient.set("test2", Map("testValue" -> List("value")))
  }

  "Routes" should {
    "return value if present in the cache" in {
      val request = HttpRequest(uri = "/test1")

      request ~> routes ~> check {
        assert(status == StatusCodes.OK)

        assert(contentType == ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] == """{}""")
      }
    }

    "return a 404 if no value is present in the cache" in {
      val request = HttpRequest(uri = "/unknown")
      request ~> routes ~> check {
        assert(handled)
        assert(status == StatusCodes.NotFound)
      }
    }

    "return a value if Akka cached endpoint is called" in {
      val request = HttpRequest(uri = "/cache/test2")
      request ~> routes ~> check {
        assert(handled)
        assert(status == StatusCodes.OK)
        assert(responseAs[String] == """Map(testValue -> List(value))""")
        redisActor
      }
    }
  }
}

