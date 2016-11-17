package com.github.lzenczuk.cn.notification

import akka.actor.{Actor, ActorLogging, ActorRef}

/**
  * Created by dev on 15/11/16.
  */

object NotificationActor {
  case object SubscribeClusterNotifications
}

class NotificationActor(out: ActorRef) extends Actor with ActorLogging{

  def receive = {
    case _ => out ! "Hello"
  }
}
