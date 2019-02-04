package com.rediscache

import akka.actor.{ Actor, ActorLogging, Props }
import com.redis.RedisClient
import com.rediscache.LocalCacheActor.SetCachedValue
import com.typesafe.config.Config

final case class CacheValue(key: String, value: Option[String])

object RedisActor {
  final case class GetValue(key: String)
  final case class CheckCache(key: String)

  def props: Props = Props[RedisActor]
}

class RedisActor extends Actor with ActorLogging {
  import RedisActor._

  var redis: RedisClient = _
  val config: Config = context.system.settings.config
  val redisConfig = config.getConfig("application.redis")

  override def preStart(): Unit = {
    try {
      log.info(s"Initializing redis connection with config: ${redisConfig.toString}")
      redis = new RedisInterface(redisConfig).client
    } catch {
      case e: Exception => {
        context.stop(sender)
        throw new Exception("Unable to initialize Redis Client: " + e.toString)
      }
    }

  }

  def receive: Receive = {
    case GetValue(key) =>
      val value = redis.get(key)
      log.info(s"$key found in Redis cache ${value.toString}")

      context.parent ! SetCachedValue(key, value)
      sender() ! value
    case _ => throw new Exception("Unidentified call to Redis actor")

  }

  override def postStop(): Unit = {
    log.info("Shutting down Redis connection")
    redis.disconnect
  }
}
