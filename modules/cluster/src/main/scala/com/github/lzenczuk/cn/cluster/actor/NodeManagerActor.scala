package com.github.lzenczuk.cn.cluster.actor

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, Address, Props}
import akka.cluster.MemberStatus._
import akka.cluster.{Cluster, MemberStatus, UniqueAddress}

/**
  * Created by dev on 14/11/16.
  */
object NodeManagerActor {
  def props = Props[NodeManagerActor]

  case class ClusterMember(address:String, roles:String, status:String)
  case class ClusterAddress(systemName: String, host: String, port: Int)

  // NodeManagerActor messages
  case object GetNodeStatus
  case class NodeStatus(address:String, inCluster:Boolean, clusterLeader:Option[String], clusterMembers: Set[ClusterMember])
  case object CreateCluster
  case class JoinCluster(clusterAddress: ClusterAddress)
  case object LeaveCluster

  def uniqueAddressToResponseString(ua:UniqueAddress):String = {
    s"${ua.address}#${ua.longUid}"
  }
}

class NodeManagerActor @Inject()(cluster: Cluster) extends Actor with ActorLogging {

  import NodeManagerActor._

  implicit def memberStatusToString(memberStatus:MemberStatus):String = {
    memberStatus match {
      case Joining => "Joining"
      case WeaklyUp => "WeaklyUp"
      case Up => "Up"
      case Leaving => "Leaving"
      case Exiting => "Exiting"
      case Down => "Down"
      case Removed => "Removed"
      case _ => "Unknown"
    }
  }

  def receive = {
    case GetNodeStatus =>

      val address: String = uniqueAddressToResponseString(cluster.selfUniqueAddress)
      val leaderOption: Option[Address] = cluster.state.leader
      val clusterMembers: Set[ClusterMember] = cluster.state.members.map(member =>
        ClusterMember(uniqueAddressToResponseString(member.uniqueAddress), member.roles.mkString(","), member.status))

      sender ! NodeStatus(address, leaderOption.isDefined, leaderOption.map(_.toString), clusterMembers)

    case JoinCluster(clusterAddress) =>
      val ca = Address("akka.tcp", clusterAddress.systemName, clusterAddress.host, clusterAddress.port)
      cluster.join(ca)

    case CreateCluster =>
      cluster.join(cluster.selfAddress)

    case LeaveCluster =>
      cluster.leave(cluster.selfAddress)
  }
}
