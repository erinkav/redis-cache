package com.rediscache

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import com.typesafe.config.Config
import akka.util.Timeout
import com.rediscache.RedisActor.GetValue

import scala.concurrent.duration._

object LocalCacheActor {
  final case class GetCachedValue(k: String)
  final case class SetCachedValue(k: String, v: Option[String])
  final case class ValueCached(k: String)
  def props: Props = Props[LocalCacheActor]
}

class LocalCacheActor extends Actor with ActorLogging {
  import LocalCacheActor._
  val config: Config = context.system.settings.config
  val localCacheConfig: Config = config.getConfig("application.localCache")
  val localLfuCache = new LocalLfuCache(localCacheConfig.getInt("capacity"), localCacheConfig.getDuration("expiry"))
  val redisActor: ActorRef = context.actorOf(RedisActor.props, "redisActor")
  implicit lazy val timeout = Timeout(5.seconds)

  def receive: Receive = {
    case GetCachedValue(key) => {
      if (localLfuCache.isDefinedAt(key)) {
        val localVal = localLfuCache.get(key)
        log.info(s"$key found in local cache")
        sender() ! localVal
      } else {
        log.info(s"$key not found in local cache")
        redisActor.forward(GetValue(key))
      }
    }
    case SetCachedValue(key, value) => {
      log.info(s"Setting $key in cache")
      localLfuCache(key) = value
    }
    case _ => {
      println("in the any block")
    }

  }

}
