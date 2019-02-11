package com.rediscache

import java.time.Duration

import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

import scala.util.Random

class LocalLfuCacheSpec extends WordSpecLike with Matchers with BeforeAndAfterAll {
  def randomStringGenerator(): String = Random.alphanumeric.take(10).mkString
  val smallCache = new LocalLfuCache(capacity = 4, globalExpiration = Duration.ofSeconds(5))
  "It should set values if not present in the cache" in {
    smallCache.set("test1", Option("value1"))

    assert(smallCache.get("test1") == Option("value1"))
  }

  "It should return None if no value exists in the cache" in {
    assert(smallCache.get("test2").isEmpty)
  }

  "It should remove the least requested value from the cache once the capacity limit is reached" in {
    smallCache.set("a", Option("1"))
    smallCache.get("a")
    smallCache.get("a")
    smallCache.set("b", Option("2"))
    smallCache.get("b")
    smallCache.set("c", Option("3"))
    smallCache.set("d", Option("3"))
    smallCache.get("d")
    smallCache.set("e", Option("5"))

    assert(smallCache.get("c").isEmpty)
  }

  "It should not return a value if expiration limit is reached" in {
    val timeConstrainedCache = new LocalLfuCache(capacity = 5, globalExpiration = Duration.ofMillis(5))
    timeConstrainedCache.set("tempVal", Option("value"))
    Thread.sleep(100)
    assert(timeConstrainedCache.get("tempVal").isEmpty)
  }
}
