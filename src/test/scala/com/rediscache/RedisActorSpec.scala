package com.rediscache

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe }
import com.redis.RedisClient
import com.rediscache.RedisActor.GetValue
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class RedisActorSpec() extends TestKit(ActorSystem("Spec")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {
  val config = ConfigFactory.load()
  val redisConfig = config.getConfig("application").getConfig("redis")
  var redisClient: RedisClient = new RedisInterface(redisConfig).client
  val cacheActor = system.actorOf(RedisActor.props)

  override def beforeAll: Unit = {
    redisClient.set("actorTest", "actorValue")
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val parent = TestProbe()

  "RedisActor" must {
    "retrieve values from the Redis cache" in {
      cacheActor ! GetValue("actorTest")
      expectMsg(Some("actorValue"))
    }

    "not throw an error if value does not exist in cache" in {
      cacheActor ! GetValue("blah")
      expectMsg(None)
    }
  }

}
