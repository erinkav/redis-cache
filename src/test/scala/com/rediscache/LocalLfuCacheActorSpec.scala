package com.rediscache

import akka.actor.{ ActorRef, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit }
import com.rediscache.LocalCacheActor.{SetCachedValue, GetCachedValue}
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

class LocalLfuCacheActorSpec() extends TestKit(ActorSystem("Spec")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {
  val config: Config = ConfigFactory.load()
  val localLfuCacheConfig: Config = config.getConfig("application").getConfig("localCache")
  val cacheActor: ActorRef = system.actorOf(LocalCacheActor.props)

  override def beforeAll: Unit = {

  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "LocalCacheActor" must {
    "Retrieve values from the local cache if they've been called before" in {
      cacheActor ! SetCachedValue("actorTest", Option("value1"))
      cacheActor ! GetCachedValue("actorTest")
      expectMsg(Some("value1"))
    }

    "Not throw an error if value does not exist in cache" in {
      cacheActor ! GetCachedValue("blah")
      expectMsg(None)
    }
  }

}
