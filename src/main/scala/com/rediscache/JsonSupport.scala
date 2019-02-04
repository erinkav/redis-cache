package com.rediscache

import com.example.UserRegistryActor.ActionPerformed
import com.rediscache.RedisActor.{ GetValue }

//#json-support
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)
  implicit val cacheValueFormat = jsonFormat1(GetValue)
  //  implicit val cacheValueStringFormat = jsonFormat1(Option[String])
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

}
