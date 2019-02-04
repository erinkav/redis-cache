//package com.rediscache
//
//import akka.actor.ActorSystem
//import akka.http.caching.LfuCache
//import akka.http.caching.scaladsl.{ Cache, CachingSettings }
//
//import scala.concurrent.{ ExecutionContext, Future }
//
//class LocalCache(localCache: Cache[String, String]) {
//  def cache: Cache[String, String] = localCache
//  // Create the route
//  def get(key: String): Future[String] = {
//    cache.getOrLoad(key, key => Future(RedisInterface.client.get(key).get))
//    //      .getOrLoad(key: String => )
//  }
//}
