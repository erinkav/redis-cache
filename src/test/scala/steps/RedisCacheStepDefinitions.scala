package steps

import akka.http.scaladsl.model.StatusCodes
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import cucumber.api.PendingException
import cucumber.api.scala.{ EN, ScalaDsl }
import org.scalatest.fixture
import scalaj.http._

class RedisCacheStepDefinitions extends ScalaDsl with EN {
  var response: HttpResponse[String] = null

  val config = ConfigFactory.load()
  val redisConfig = config.getConfig("application.redis")
  val redisClient = new RedisClient(redisConfig.getString("host"), redisConfig.getInt("port"))
  val uri = s"${config.getString("application.host")}:${config.getInt("application.port")}"
  redisClient.flushall
  var cacheKey: String = null
  var cacheVal: String = null

  Given("""^an API request to retrieve a "([^"]*)" that "([^"]*)" exists in the cache with value "([^"]*)"$""") { (key: String, existCondition: String, value: String) =>
    println(existCondition)
    existCondition match {
      case "does_not" => assert(redisClient.get(key).isEmpty)
      case "does" => redisClient.set(key, value)
    }
    cacheKey = key
    cacheVal = value
  }

  When("""^GET is called with a "([^"]*)" endpoint$""") { (validCondition: String) =>
    response = Http(s"http://$uri/$cacheKey").asString
  }

  Then("""^it should return a (\d+)$""") { (statusCode: Int) =>
    assert(response.code == statusCode, s"Status code ${response.code} did not match expected $statusCode")
  }

  Then("""^it "([^"]*)" exist in the Redis cache$""") { (shouldCondition: String) =>
    shouldCondition match {
      case "should" => assert(redisClient.get(cacheKey).contains(cacheVal))
      case "should_not" => assert(redisClient.get(cacheKey).isEmpty)
    }

  }

  Then("""^it "([^"]*)" return a response "([^"]*)"$""") { (shouldCondition: String, responseValue: String) =>
    shouldCondition match {
      case "should_not" => assert(response.code != 200, s"Expected error response but got: ${response.toString}")
      case "should" => assert(response.body == responseValue, s"Body did not equal response value: " +
        s"$responseValue did not match ${response.body}")
    }
  }

}
