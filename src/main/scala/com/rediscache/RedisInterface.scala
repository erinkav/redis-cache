package com.rediscache

import com.redis.RedisClient
import com.typesafe.config.Config

class RedisInterface(config: Config) {

  val host: String = config.getString("host")
  val port: Int = config.getInt("port")

  def client: RedisClient = new RedisClient(host, port)

  def get(key: String): Option[String] = {
    client.get(key)
  }

  def set(key: String, value: String): Unit = {
    client.set(key, value)
  }

  def clear(): Unit = {
    client.flushall
  }

  def disconnect(): Unit = {
    client.disconnect
  }
}
