package com.github.lzenczuk.cn.cluster.domain

/**
  * Created by dev on 17/11/16.
  */

case class NodeId(protocol: String, system: String, host: Option[String], port: Option[Int], uid: Long)

trait ApplicationCluster {
  def updateNode(nodeId: NodeId, nodeState: NodeState, leader: Boolean): ClusterChange
  def updateNode(nodeId: NodeId, nodeState: NodeState): ClusterChange
  def updateNode(nodeId: NodeId, leader: Boolean): ClusterChange
  def get():ClusterChange
  def reset
}
