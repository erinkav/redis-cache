package com.rediscache

import com.redis._
import com.typesafe.config.ConfigFactory

object RedisInterface {
  val redisConfig = ConfigFactoryHelper.envConfig
  println(redisConfig)
  val config = redisConfig.getConfig("redis")
  println(config)
  println(redisConfig)
  println(redisConfig)
  val host: String = config.getString("host")
  val port: Int = config.getInt("port")

  println(host, port)

  def client: RedisClient = new RedisClient("localhost", 6379)
  def get(key: String): Option[String] = {
    println("get" + key)
    client.get(key)
  }
}
