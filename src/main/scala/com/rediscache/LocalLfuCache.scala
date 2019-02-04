package com.rediscache

import scala.collection.concurrent.TrieMap
import java.time.Duration

trait LocalCache[K, V] {
  private val _cache: TrieMap[K, V] = TrieMap[K, V]()

  def add(k: K, v: V): Unit = _cache.putIfAbsent(k, v)

  def get(k: K): Option[V] = _cache.get(k)

  def delete(k: K): Unit = _cache -= k

}

class CacheNode[K, V](_key: K, _value: V, validTime: Long) {
  val value: V = _value
  val expiryTime: Long = System.currentTimeMillis() + validTime
  var accessCount: Int = 1
  var frequency: Int = 1
}

class LocalLfuCache(capacity: Int, globalExpiration: Duration) {
  val counts: TrieMap[String, Int] = TrieMap()
  val valuesMap: TrieMap[String, CacheNode[String, Option[String]]] = TrieMap()
  val frequencies: TrieMap[Int, List[String]] = TrieMap()
  val max: Int = capacity
  val expiration: Long = globalExpiration.toMillis

  def get(key: String): Option[String] = {
    if (valuesMap.isDefinedAt(key) && valuesMap(key).expiryTime > System.currentTimeMillis()) {
      val cacheNode = valuesMap(key)

      val frequency = counts(key)
      val updatedList = frequencies(frequency).filter(frequencyKey => key != frequencyKey)

      val newFrequencyList = frequencies.getOrElse(frequency + 1, List[String]()) ++ List(key)

      counts(key) = frequency + 1
      if (updatedList.isEmpty) {
        frequencies -= frequency
      } else {
        frequencies(frequency) = updatedList
      }

      frequencies(frequency + 1) = newFrequencyList
      valuesMap(key).value
    } else {
      None
    }
  }

  def set(key: String, value: Option[String]): Unit = {
    if (!valuesMap.isDefinedAt(key)) {
      if (valuesMap.size == max) {
        val lowestCount = frequencies.keySet.min
        frequencies.foreach(freq => println(freq._1, freq._2))
        val leastUsedKeys = frequencies(lowestCount)
        val removedKey = leastUsedKeys.head
        frequencies(lowestCount) = leastUsedKeys.tail
        if (frequencies(lowestCount).isEmpty) frequencies.remove(lowestCount)
        valuesMap.remove(removedKey)
        counts.remove(removedKey)
      }

      valuesMap(key) = new CacheNode(key, value, expiration)
      counts(key) = 1
      frequencies(1) = frequencies.getOrElse(1, List[String]()) ++ List(key)
    }
  }

}
