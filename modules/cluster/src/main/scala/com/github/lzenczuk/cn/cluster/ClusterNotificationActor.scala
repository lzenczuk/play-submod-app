package com.github.lzenczuk.cn.cluster

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, Terminated}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.routing.{BroadcastRoutingLogic, Router}


/**
  * Created by dev on 15/11/16.
  */

object ClusterNotificationActor {
  case object SubscribeClusterStatus
}

class ClusterNotificationActor @Inject() (cluster: Cluster) extends Actor with ActorLogging {
  import ClusterNotificationActor._

  var broadcastRouter = Router(BroadcastRoutingLogic())
  var clusterStatus:AppCluster = new AppCluster


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      classOf[MemberEvent]
    )
  }

  def receive = {
    case SubscribeClusterStatus =>
      sender ! clusterStatus
      context.watch(sender)
      broadcastRouter = broadcastRouter.addRoutee(sender)
    case Terminated(subject) =>
      broadcastRouter = broadcastRouter.removeRoutee(sender)
    case currentClusterState:CurrentClusterState =>
      val cs = currentClusterState.members.foldLeft(List[AppClusterNodeChange]()){ (statuses, member) =>
        statuses ++ clusterStatus.updateStatus(member.uniqueAddress, member.status)
      }
      cs.foreach(cms => broadcastRouter.route(cms, self))
    case memberEvent: MemberEvent =>
      val statuses = clusterStatus.updateStatus(memberEvent.member.uniqueAddress, memberEvent.member.status)
      statuses.foreach(cms => broadcastRouter.route(cms, self))
  }

}
