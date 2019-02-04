package com.rediscache

//#user-routes-spec
//#test-top
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.redis.RedisClient
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scalatest.concurrent.ScalaFutures

//#set-up
class RoutesSpec extends WordSpec with BeforeAndAfterAll with Matchers with ScalaFutures with ScalatestRouteTest
  with Routes {
  //#test-top

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe()
  override val redisActor: ActorRef =
    system.actorOf(RedisActor.props, "cacheActor")

  lazy val routes = route
  var redisClient: RedisClient = RedisInterface.client
  override def beforeAll() {
    redisClient.set("test1", "{}")
    redisClient.set("test2", Map("testValue" -> List("value")))
  }

  "Routes" should {
    //    "return None if no value is present in the cache (GET /get)" in {
    // note that there's no need for the host part in the uri:
    //      val request = HttpRequest(uri = "/get/nonexistent")
    //
    //      request ~> routes ~> check {
    //        status should ===(StatusCodes.OK)
    //
    //        // we expect the response to be json:
    //        contentType should ===(ContentTypes.`application/json`)
    //
    //        // and no entries should be in the list:
    //        entityAs[String] should ===("""None""")
    //      }
    "return value if present in the cache" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/test1")

      request ~> routes ~> check {
        assert(status == StatusCodes.OK)

        // we expect the response to be json:
        assert(contentType == ContentTypes.`application/json`)
        assert(entityAs[String] == """{}""")
      }
    }

    "return empty list if no value is present in the cache" in {
      val request = HttpRequest(uri = "/unknown")
      request ~> routes ~> check {
        assert(handled)
        assert(status == StatusCodes.OK)
        println(response)
        assert(responseAs[String] == "[]")
        redisActor
      }

    }

    "throw an error if requested with unsupported call" in {
      Post() ~> routes ~> check {
        assert(status == StatusCodes.MethodNotAllowed)
      }
    }

  }
  //#actual-test

  //#set-up
}
//#set-up
//#user-routes-spec
