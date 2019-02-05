package steps

import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import cucumber.api.PendingException
import cucumber.api.scala.{ EN, ScalaDsl }
import scalaj.http._

class RedisCacheStepDefinitions extends ScalaDsl with EN {
  var response: HttpResponse[String] = null
  val uri = "redis-cache:8080"
  val config = ConfigFactory.load().getConfig("application.redis")
  val redisClient = new RedisClient(config.getString("host"), config.getInt("port"))
  redisClient.flushall
  var cacheKey: String = null
  var cacheVal: String = null

  Given("""an API request to retrieve a {string} that {string} exists in the cache with value {string}""") { (key: String, existCondition: String, value: String) =>
    println(existCondition)
    existCondition match {
      case "does_not" => assert(redisClient.get(key).isEmpty)
      case "does" => redisClient.set(key, value)
    }
    cacheKey = key
    cacheVal = value
  }

  When("""GET is called with a {string} endpoint""") { (validCondition: String) =>
    response = Http(s"http://$uri/$cacheKey").asString
  }

  Then("""it should return a {int}""") { (statusCode: Int) =>
    assert(response.code == statusCode, s"Status code ${response.code} did not match expected $statusCode")
  }

  Then("""it should return a response {string}""") { (responseValue: String) =>
    responseValue match {
      case "None" => assert(response.body.isEmpty)
      case _ => assert(response.body == responseValue)
    }
  }

  Then("""it {string} exist in the Redis cache""") { (shouldCondition: String) =>
    shouldCondition match {
      case "should" => assert(redisClient.get(cacheKey).get == cacheVal)
      case "should_not" => assert(redisClient.get(cacheKey) == None)
    }
  }

}
