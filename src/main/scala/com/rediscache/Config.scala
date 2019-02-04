package com.rediscache

import com.typesafe.config.{ Config, ConfigFactory }

object ConfigFactoryHelper {
  val config: Config = ConfigFactory.load()
  val envConfig: Config = config.getConfig("dev")
}
