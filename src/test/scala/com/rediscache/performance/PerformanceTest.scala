package com.rediscache.performance

import com.redis.RedisClient
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class PerformanceTest extends Simulation {
  val userCount: Int = 10
  val maxUserCount: Int = 50
  val testDuration: FiniteDuration = 10.seconds
  val maxReponseTime: Int = 50
  val percentSuccess: Int = 100
  val redisHost: String = "localhost"
  val redisPort: Int = 6379
  val httpProtocol = http
    .baseUrl("http://127.0.0.1:8080")
    .check(status.is(200))

  before {
    val testClient = new RedisClient(redisHost, redisPort)
    testClient.connect
    testClient.set("A", "B")
    testClient.set("B", "C")
  }

  object GET {
    val akkaCache = exec(http("AkkaLFUEndpointCached").get("/cache/A")).pause(1)
      .exec(http("AkkaLFUEndpointNotCached").get("/cache/C"))

    val localCache = exec(http("LocalLFUEndpointCached").get("/A")).pause(1)
      .exec(http("LocalLFUEndpointNotCached").get("/B"))
  }

  val testScenario = scenario("callCacheEndpoints").exec(GET.akkaCache)
  setUp(testScenario.inject(rampConcurrentUsers(userCount) to maxUserCount during testDuration)
    .protocols(httpProtocol))
    .assertions(
      global.successfulRequests.percent.gte(percentSuccess),
      forAll.responseTime.max.lt(maxReponseTime))

}