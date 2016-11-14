package com.github.lzenczuk.cn.cluster

import akka.actor.{Actor, ActorLogging, Props}

/**
  * Created by dev on 14/11/16.
  */
object NodeManagerActor {
  def props = Props[NodeManagerActor]

}

class NodeManagerActor extends Actor with ActorLogging {

  def receive = {
    case s:String =>
      log.info(s"Receive message: $s")
      sender ! "NodeManagerResponse"
  }
}
