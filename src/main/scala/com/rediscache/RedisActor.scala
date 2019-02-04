package com.rediscache

//#user-registry-actor
import akka.actor.{ Actor, ActorLogging, Props }

//#user-case-classes
final case class User(name: String, age: Int, countryOfResidence: String)
final case class Users(users: Seq[User])
final case class CacheValue(key: String, value: Option[String])
//#user-case-classes

object RedisActor {
  final case class ActionPerformed(description: String)
  final case class GetValue(key: String)
  final case class CheckCache(key: String)

  def props: Props = Props[RedisActor]
}

class RedisActor extends Actor with ActorLogging {
  import RedisActor._

  var users = Set.empty[User]

  def receive: Receive = {
    case GetValue(key) =>
      println("got here" + key)
      val value = RedisInterface.get(key)
      println("in get value" + value)

      sender() ! value
    case CheckCache(key) =>
      println("checking cache first")

    case _ =>
      println("got to blank")
      sender() ! "done?"
  }
}
//#user-registry-actor