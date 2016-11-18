package com.github.lzenczuk.cn.cluster.domain

/**
  * Created by dev on 17/11/16.
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

case class NodeChange(protocol: String, system: String, host: Option[String], port: Option[Int], uid: Long, nodeState: NodeState, leader: Boolean)
case class ClusterChange(changes:List[NodeChange] = List()){

  /**
    * Create new ClusterChange containing changes from both, this and provided as parameter.
    * @param clusterChange ClusterChange containing changes to add to this
    * @return new ClusterChange
    */
  def ++ (clusterChange: ClusterChange):ClusterChange = ClusterChange(changes ++ clusterChange.changes)
}
