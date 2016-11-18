package com.github.lzenczuk.cn.cluster.actor

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, ActorRef, Address, Terminated}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import akka.routing.{BroadcastRoutingLogic, Router}
import com.github.lzenczuk.cn.cluster.domain.{ApplicationCluster, ClusterChange, NodeId, NodeState}


/**
  * Created by dev on 15/11/16.
  */

object ClusterNotificationActor {
  case object SubscribeClusterStatus
}

class ClusterNotificationActor @Inject() (akkaCluster: Cluster, applicationCluster:ApplicationCluster) extends Actor with ActorLogging {
  import ClusterNotificationActor._

  var broadcastRouter = Router(BroadcastRoutingLogic())

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    akkaCluster.subscribe(self, classOf[MemberEvent], classOf[LeaderChanged])
  }

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    akkaCluster.unsubscribe(self)
  }


  @scala.throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    akkaCluster.subscribe(self, classOf[MemberEvent], classOf[LeaderChanged])
  }

  implicit def addressToNodeId(address: Address):NodeId = {
    NodeId(address.protocol, address.system, address.host, address.port)
  }

  implicit def memberStatusToNodeStatus(memberStatus: MemberStatus):NodeState = {
    memberStatus match {
      case MemberStatus.Joining => NodeState.Joining
      case MemberStatus.WeaklyUp => NodeState.WeaklyUp
      case MemberStatus.Up => NodeState.Up
      case MemberStatus.Leaving => NodeState.Leaving
      case MemberStatus.Exiting => NodeState.Exiting
      case MemberStatus.Down => NodeState.Down
      case MemberStatus.Removed => NodeState.Removed
      case _ => NodeState.Unknown
    }
  }

  def receive = {
    case currentClusterState:CurrentClusterState =>

      applicationCluster.reset

      val cc = currentClusterState.members.foldLeft(ClusterChange()){ (clusterChange, member) =>
        clusterChange ++ applicationCluster.updateNode(member.uniqueAddress.address, member.uniqueAddress.longUid, member.status)
      }
      broadcastRouter.route(cc, self)

    case SubscribeClusterStatus =>
      sender ! applicationCluster.get()
      context.watch(sender)
      broadcastRouter = broadcastRouter.addRoutee(sender)
    case Terminated(subject:ActorRef) =>
      broadcastRouter = broadcastRouter.removeRoutee(subject)

    case memberEvent: MemberEvent =>
      val cc = applicationCluster.updateNode(memberEvent.member.uniqueAddress.address, memberEvent.member.uniqueAddress.longUid, memberEvent.member.status)
      broadcastRouter.route(cc, self)

    case leaderChanged:LeaderChanged =>
      leaderChanged.leader.foreach(address => {
        val cc = applicationCluster.updateNode(address, true)
        broadcastRouter.route(cc, self)
      })

  }
}
