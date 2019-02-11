package com.rediscache

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RequestContext, RouteResult }
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.rediscache.RedisActor.GetValue
import akka.http.scaladsl.server.directives.CachingDirectives._
import com.rediscache.LocalCacheActor.GetCachedValue
import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.Future
import scala.concurrent.duration._

trait Routes {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  def redisActor: ActorRef
  def localCacheActor: ActorRef

  val keyerFunction: PartialFunction[RequestContext, Uri] = {
    case r: RequestContext ⇒ r.request.uri
  }
  val cacheConfig: Config = ConfigFactory.load()

  val defaultCachingSettings = CachingSettings(cacheConfig)
  val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings
  val cachingSettings = defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val akkaLfuCache: Cache[Uri, RouteResult] = LfuCache(cachingSettings)

  implicit lazy val timeout = Timeout(5.seconds)

  // Two route options:
  // GET /cache/* to hit the routing cache Akka implementation. LFU settings are configured in the akka settings in the config
  // Get /* to hit the localLFUCache implementation
  def route = {
    concat(
      get {
        path(Segment) { key =>
          val cachedValue: Future[Option[String]] = (localCacheActor ? GetCachedValue(key)).mapTo[Option[String]]
          onSuccess(cachedValue) {
            case None => {
              complete(StatusCodes.NotFound)
            }
            case Some(v) => {
              complete((StatusCodes.OK, v))
            }
            case _ => {
              println("WHAT")
              complete(StatusCodes.InternalServerError)
            }
          }
        }
      },
      pathPrefix("cache") {
        get {
          path(Segment) { key =>
            cache(akkaLfuCache, keyerFunction) {
              val cacheValue: Future[Option[String]] =
                (redisActor ? GetValue(key)).mapTo[Option[String]]
              onSuccess(cacheValue) {
                case None => {
                  complete(StatusCodes.NotFound)
                }
                case Some(v) => {
                  complete((StatusCodes.OK, v))
                }
              }
            }
          }
        }
      })
  }

}
