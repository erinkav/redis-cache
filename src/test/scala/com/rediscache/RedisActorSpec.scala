package com.rediscache

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActors, TestKit }
import com.redis.RedisClient
import com.rediscache.RedisActor.GetValue
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

//class MySpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
//  with WordSpecLike with Matchers with BeforeAndAfterAll {
//
//  override def afterAll: Unit = {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  "An Echo actor" must {
//
//    "send back messages unchanged" in {
//      val echo = system.actorOf(TestActors.echoActorProps)
//      echo ! "hello world"
//      expectMsg("hello world")
//    }
//
//  }
//}
class RedisActorSpec() extends TestKit(ActorSystem("Spec")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  var redisClient: RedisClient = RedisInterface.client
  val cacheActor = system.actorOf(RedisActor.props)

  override def beforeAll: Unit = {
    redisClient.set("actorTest", "actorValue")
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Redis actor" must {
    "Retrieve values from the Redis cache" in {
      cacheActor ! GetValue("actorTest")
      expectMsg(Some("actorValue"))
    }

    "Not throw an error if value does not exist in cache" in {
      cacheActor ! GetValue("blah")
      expectMsg(None)
    }

  }

}
