package com.github.lzenczuk.cn.cluster.domain.impl

import com.github.lzenczuk.cn.cluster._
import com.github.lzenczuk.cn.cluster.domain._

/**
  * Created by dev on 17/11/16.
  */

class ApplicationClusterImpl extends ApplicationCluster{

  class ApplicationNode(nodeId: NodeId, var nodeState: NodeState, var leader: Boolean){

    def update(isLeader: Boolean):NodeChange = {
      leader = isLeader
      toNodeChange
    }

    def update(ns: NodeState, isLeader: Boolean):NodeChange = {
      nodeState = ns
      leader = isLeader
      toNodeChange
    }

    def toNodeChange:NodeChange = NodeChange(nodeId.protocol, nodeId.system, nodeId.host, nodeId.port, nodeId.uid, nodeState, leader)
  }

  var leader:Option[NodeId] = None
  var members:Map[NodeId, ApplicationNode] = Map()

  override def updateNode(nodeId: NodeId, isNodeLeader: Boolean): ClusterChange = {
    val changes = scala.collection.mutable.MutableList[NodeChange]()

    changes ++= updateLeader(nodeId, isNodeLeader)
    changes ++= updateNodeState(nodeId, getNodeState(nodeId), isNodeLeader)

    ClusterChange(changes.toList)
  }

  override def updateNode(nodeId: NodeId, nodeState: NodeState): ClusterChange = {
    val changes = scala.collection.mutable.MutableList[NodeChange]()

    changes ++= updateNodeState(nodeId, nodeState, getIsNodeLeader(nodeId))

    ClusterChange(changes.toList)
  }

  override def updateNode(nodeId: NodeId, nodeState: NodeState, isNodeLeader: Boolean): ClusterChange = {
    val changes = scala.collection.mutable.MutableList[NodeChange]()

    changes ++= updateLeader(nodeId, isNodeLeader)
    changes ++= updateNodeState(nodeId, nodeState, isNodeLeader)

    ClusterChange(changes.toList)
  }

  override def get(): ClusterChange = {
    ClusterChange(members.map(entry => entry._2).map(_.toNodeChange).toList)
  }

  override def reset: Unit = {
    leader = None
    members = Map()
  }

  private def updateLeader(nodeId: NodeId, isNodeLeader: Boolean):List[NodeChange] = {
    var change = List[NodeChange]()

    if(isNodeLeader && leader.isDefined && nodeId!=leader.get){
      change = List(members(leader.get).update(false))
    }

    if(isNodeLeader){
      leader = Some(nodeId)
    }else if(!isNodeLeader && leader.exists(nid => nid==nodeId)){
      leader = None
    }

    change
  }

  private def updateNodeState(nodeId: NodeId, nodeState: NodeState, isNodeLeader: Boolean):List[NodeChange] = {
    var change = List[NodeChange]()

    if(!members.contains(nodeId)){
      val newNode: ApplicationNode = new ApplicationNode(nodeId, nodeState, isNodeLeader)
      members = members + (nodeId -> newNode)
      change = List(newNode.toNodeChange)
    }else{
      change = List(members(nodeId).update(nodeState, isNodeLeader))
    }

    change
  }

  private def getNodeState(nodeId: NodeId):NodeState = {
    members.get(nodeId).map(_.nodeState).getOrElse(NodeState.Unknown)
  }

  private def getIsNodeLeader(nodeId: NodeId):Boolean = {
    members.get(nodeId).exists(_.leader)
  }
}
