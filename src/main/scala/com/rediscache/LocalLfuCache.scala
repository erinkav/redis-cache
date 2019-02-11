package com.rediscache

import scala.collection.concurrent.TrieMap
import java.time.Duration

trait LocalCache[K, V] {
  private val _cache: TrieMap[K, V] = TrieMap[K, V]()

  def add(k: K, v: V): Unit = _cache.putIfAbsent(k, v)

  def get(k: K): Option[V] = _cache.get(k)

  def delete(k: K): Unit = _cache -= k

}

class CacheNode[K, V](_key: K, _value: V, validTime: Long, frequencyNode: FrequencyNode) {
  val value: V = _value
  val expiryTime: Long = System.currentTimeMillis() + validTime
  var freqNode: FrequencyNode = frequencyNode
}

class FrequencyNode(_key: String) {
  val key: String = _key
  var prev: FrequencyNode = _
  var next: FrequencyNode = _

  def reset(): Unit = {
    prev = null
    next = null
  }
}

class FrequencyDoubleLinkedList() {
  var head: FrequencyNode = _
  var tail: FrequencyNode = _
  var size: Int = 0
  def add(node: FrequencyNode): Unit = {
    if (head == null) {
      head = node
    } else if (tail == null) {
      head.next = node
      tail = node
      tail.prev = head
    } else {
      val currTail = tail
      currTail.next = node
      tail = node
      tail.prev = currTail
    }
    size += 1
  }

  def delete(node: FrequencyNode): Unit = {
    if (node == head) {
      head = head.next
      if (head != null) {
        head.prev = null
      }
    } else if (node == tail) {
      tail = node.prev
      tail.next = null
    } else {
      node.prev.next = node.next
    }
    size -= 1
  }

  def isEmpty: Boolean = size == 0
}

class LocalLfuCache(capacity: Int, globalExpiration: Duration) {
  // TrieMap allows for concurrent access and updating

  // Map to track count of times requested with lookup by key value
  val counts: TrieMap[String, Int] = TrieMap()
  // Map to track the key and associated value. Each value stores its own expiry time
  val valuesMap: TrieMap[String, CacheNode[String, Option[String]]] = TrieMap()
  // Map to track frequency of lookups. Each cacheNode is stored in a frequency list
  val frequencies: TrieMap[Int, FrequencyDoubleLinkedList] = TrieMap()

  // Max capacity and expiration are configurable
  val max: Int = capacity
  val expiration: Long = globalExpiration.toMillis

  def get(key: String): Option[String] = {
    if (valuesMap.isDefinedAt(key)) {
      val cacheNode = valuesMap(key)
      val frequency = counts(key)

      // delete node from the list it was previously in. Update node's pointers to null once removed
      updateFrequenciesList(frequency, cacheNode.freqNode)
      cacheNode.freqNode.reset()
      // checks if value is valid on look up. If it's no longer valid it should be removed from the cache
      if (valuesMap(key).expiryTime > System.currentTimeMillis()) {
        // Updated frequency count of times it's been accessed.
        val newFrequencyList = frequencies.getOrElse(frequency + 1, new FrequencyDoubleLinkedList())
        newFrequencyList.add(cacheNode.freqNode)

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

        // Get first node in frequencies list and delete it
        val removedFrequencyNode = leastUsedKeys.head
        val removedKey = removedFrequencyNode.key
        updateFrequenciesList(lowestCount, removedFrequencyNode)

        valuesMap.remove(removedKey)
        counts.remove(removedKey)
      }

      // Add new key to values, count and frequencies map
      counts(key) = 1
      // Get list of other values at frequency or create a new list if none exist
      val existingFrequencyList = frequencies.getOrElse(1, new FrequencyDoubleLinkedList())
      // Create new node representing frequency. Stores position in frequency list
      val newFrequencyNode = new FrequencyNode(key)
      // Add frequency node to end of frequency list
      existingFrequencyList.add(newFrequencyNode)
      frequencies(1) = existingFrequencyList
      // Create new cache node, storing frequency node, expiration and node value
      valuesMap(key) = new CacheNode(key, value, expiration, newFrequencyNode)
    }
  }

  // Removes cache values from frequencies list. If the list is empty the frequency key will be removed
  def updateFrequenciesList(frequency: Int, node: FrequencyNode): Unit = {
    val frequencyList = frequencies(frequency)
    // call delete on node - doubly linked list for constant time removals
    frequencyList.delete(node)

    // remove list from frequencies map if none is stored
    if (frequencyList.isEmpty) {
      frequencies -= frequency
    }
  }

}
