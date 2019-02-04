package com.rediscache

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
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
import com.rediscache.LocalCacheActor.{ GetCachedValue, SetCachedValue }
import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.Future
import scala.concurrent.duration._

//#user-routes-class
trait Routes {
  //#user-routes-class

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  //  // other dependencies that UserRoutes use
  def redisActor: ActorRef
  def localCacheActor: ActorRef

  val keyerFunction: PartialFunction[RequestContext, Uri] = {
    case r: RequestContext ⇒ r.request.uri
  }
  val cacheConfig: Config = ConfigFactory.load()

  println("cache" + cacheConfig)
  val defaultCachingSettings = CachingSettings(cacheConfig)
  val lfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(25)
      .withMaxCapacity(50)
      .withTimeToLive(20.seconds)
      .withTimeToIdle(10.seconds)
  val cachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val akkaLfuCache: Cache[Uri, RouteResult] = LfuCache(cachingSettings)

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds)
  def route = {
    concat(
      pathPrefix("cache") {
        get {
          path(Segment) { key =>
            val cachedValue: Future[Option[String]] = (localCacheActor ? GetCachedValue(key)).mapTo[Option[String]]
            onSuccess(cachedValue) { returnedVal =>
              log.info("Got value" + returnedVal)
              localCacheActor ! SetCachedValue(key, returnedVal)
              complete(returnedVal)
            }
          }
        }

      },
      pathEnd {
        get {
          path(Segment) { key =>
            cache(akkaLfuCache, keyerFunction) {
              val cacheValue: Future[Option[String]] =
                (redisActor ? GetValue(key)).mapTo[Option[String]]
              println("cacheGet called" + cacheValue)
              onSuccess(cacheValue) { returnedVal =>
                log.info("Got value" + returnedVal)
                //                if (returnedVal != None) {
                //                  log.info(s"Setting $key in local cache to $returnedVal")
                //                  localCacheActor ! SetCachedValue(key, returnedVal)
                //                }
                complete(returnedVal)
              }
            }
          }
        }
      })

  }

  //#all-routes
}
