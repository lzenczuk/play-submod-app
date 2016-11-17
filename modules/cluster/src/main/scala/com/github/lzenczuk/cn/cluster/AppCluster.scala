package com.github.lzenczuk.cn.cluster

import akka.cluster.{MemberStatus, UniqueAddress}

/**
  * Created by dev on 15/11/16.
  */

sealed abstract class NodeState

object NodeState {

  case object Joining extends NodeState

  case object WeaklyUp extends NodeState

  case object Up extends NodeState

  case object Leaving extends NodeState

  case object Exiting extends NodeState

  case object Down extends NodeState

  case object Removed extends NodeState

  case object Unknown extends NodeState

}

case class AppClusterNodeChange(protocol: String, system: String, host: Option[String], port: Option[Int], uid: Long, nodeState: NodeState, leader: Boolean)

case class AppClusterChange(members: Set[AppClusterNodeChange])

class AppClusterNode(
                      val protocol: String,
                      val system: String,
                      val host: Option[String],
                      val port: Option[Int],
                      val uid: Long,
                      var nodeState: NodeState,
                      var leader: Boolean
                    ) {

  def changeNodeState(ns: NodeState): AppClusterNodeChange = {
    nodeState = ns
    toAppClusterNodeChange
  }

  def changeClusterLeader(isLeader: Boolean): AppClusterNodeChange = {
    leader = isLeader
    toAppClusterNodeChange
  }

  def toAppClusterNodeChange: AppClusterNodeChange = AppClusterNodeChange(protocol, system, host, port, uid, nodeState, leader)
}

class AppCluster {

  type NodeKey = String

  implicit def uniqueAddressToNodeKey(ua: UniqueAddress): NodeKey = ua.address.protocol + ua.address.system + ua.address.host + ua.address.port + ua.longUid

  implicit def memberStatusToNodeStatus(memberStatus: MemberStatus): NodeState = {
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

  var leader: Option[NodeKey] = None
  var members: Map[NodeKey, AppClusterNode] = Map()

  def updateStatus(uniqueAddress: UniqueAddress, memberStatus: MemberStatus): List[AppClusterNodeChange] = {

    val status = new AppClusterNode(
      uniqueAddress.address.protocol,
      uniqueAddress.address.system,
      uniqueAddress.address.host,
      uniqueAddress.address.port,
      uniqueAddress.longUid,
      memberStatus,
      leader.contains(uniqueAddress))

    if (MemberStatus.down.equals(memberStatus)) {
      members = members - uniqueAddress
      List(status.toAppClusterNodeChange)
    } else {

      members = members + (uniqueAddressToNodeKey(uniqueAddress) -> status)
      List(status.toAppClusterNodeChange)
    }
  }

  def setLeader(uniqueAddress: UniqueAddress): List[AppClusterNodeChange] = {

    val newLeaderKey = uniqueAddress
    var statuses = List[AppClusterNodeChange]()

    if (leader.isDefined && members.contains(leader.get)) {
      statuses = members(leader.get).changeClusterLeader(false) :: statuses
    }

    if (members.contains(newLeaderKey)) {
      statuses = members(newLeaderKey).changeClusterLeader(true) :: statuses
    }

    leader = Some(newLeaderKey)
    statuses
  }
}
