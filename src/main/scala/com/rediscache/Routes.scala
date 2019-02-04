package com.rediscache

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ StatusCodes, Uri }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ RequestContext, Route, RouteResult }
import akka.http.scaladsl.server.directives.MethodDirectives.{ delete, get, post }
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.rediscache.RedisActor.GetValue
import akka.http.scaladsl.server.directives.CachingDirectives._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.duration._

//#user-routes-class
trait Routes extends JsonSupport {
  //#user-routes-class

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[Routes])

  //  // other dependencies that UserRoutes use
  def redisActor: ActorRef

  val keyerFunction: PartialFunction[RequestContext, Uri] = {
    case r: RequestContext ⇒ r.request.uri
  }
  val cacheConfig = ConfigFactory.load()
  val defaultCachingSettings = CachingSettings(cacheConfig)
  val lfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(25)
      .withMaxCapacity(50)
      .withTimeToLive(20.seconds)
      .withTimeToIdle(10.seconds)
  val cachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)
  val lfuCache: Cache[Uri, RouteResult] = LfuCache(cachingSettings)

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  def route = {
    get {
      path(Segment) { key =>
        //        cache(lfuCache, keyerFunction) {
        val cacheValue: Future[Option[String]] =
          (redisActor ? GetValue(key)).mapTo[Option[String]]
        println("cacheGet called" + cacheValue)
        complete(cacheValue)
        //        onSuccess(cacheValue) { returnedVal =>
        //          log.info("Got value" + returnedVal)
        //          complete(returnedVal)
        //        }
        //        }
      }
    }
    pathPrefix("cache") {
      path(Segment) { key =>
        // check lfu implementation
        complete("cache")
      }
    }

  }

  //#all-routes
}
