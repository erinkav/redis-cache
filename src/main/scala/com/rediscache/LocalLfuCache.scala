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
}

class LocalLfuCache(capacity: Int, globalExpiration: Duration) {
  // TrieMap allows for concurrent access and updating

  // Map to track count of times requested with lookup by key value
  val counts: TrieMap[String, Int] = TrieMap()
  // Map to track the key and associated value. Each value stores its own expiry time
  val valuesMap: TrieMap[String, CacheNode[String, Option[String]]] = TrieMap()
  // Map to track frequency of lookups. Each cacheNode is stored in a frequency list
  val frequencies: TrieMap[Int, List[String]] = TrieMap()

  // Max capacity and expiration are configurable
  val max: Int = capacity
  val expiration: Long = globalExpiration.toMillis

  def get(key: String): Option[String] = {
    if (valuesMap.isDefinedAt(key)) {
      val cacheNode = valuesMap(key)
      val frequency = counts(key)

      updateFrequenciesList(frequency, key)
      // checks if value is valid on look up. If it's no longer valid it should be removed from the cache
      if (valuesMap(key).expiryTime > System.currentTimeMillis()) {
        // Updated frequency count of times it's been accessed.
        // Can be a linear time operation at worst if all keys are accessed the same number of times
        val newFrequencyList = frequencies.getOrElse(frequency + 1, List[String]()) ++ List(key)

        counts(key) = frequency + 1
        frequencies(frequency + 1) = newFrequencyList
        valuesMap(key).value
      } else {
        // Remove expired value
        valuesMap -= key
        counts -= key
        None
      }
    } else {
      None
    }
  }

  def set(key: String, value: Option[String]): Unit = {
    // If value is already defined don't set it
    if (!valuesMap.isDefinedAt(key)) {
      // check if cache is at limit. If so evict oldest values
      if (valuesMap.size == max) {
        // The first value in the lowest frequency list is the one to evict
        val lowestCount = frequencies.keySet.min
        val leastUsedKeys = frequencies(lowestCount)
        val removedKey = leastUsedKeys.head

        frequencies(lowestCount) = leastUsedKeys.tail
        if (frequencies(lowestCount).isEmpty) frequencies.remove(lowestCount)
        valuesMap.remove(removedKey)
        counts.remove(removedKey)
      }
      // Add new key to values, count and frequencies map
      valuesMap(key) = new CacheNode(key, value, expiration)
      counts(key) = 1
      frequencies(1) = frequencies.getOrElse(1, List[String]()) ++ List(key)
    }
  }

  // Removes cache values from frequencies list. If the list is empty the frequency key will be removed
  def updateFrequenciesList(frequency: Int, key: String): Unit = {
    // Can be a linear time operation at worst if many keys are stored at the same frequency
    val updatedFrequencyList = frequencies(frequency).filter(frequencyKey => key != frequencyKey)

    if (updatedFrequencyList.isEmpty) {
      frequencies -= frequency
    } else {
      frequencies(frequency) = updatedFrequencyList
    }
  }

}
