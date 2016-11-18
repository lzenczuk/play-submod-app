package com.github.lzenczuk.cn.cluster.domain

/**
  * Created by dev on 17/11/16.
  */

case class NodeId(protocol: String, system: String, host: Option[String], port: Option[Int])

trait ApplicationCluster {
  def updateNode(nodeId: NodeId, uid: Long, nodeState: NodeState): ClusterChange
  def updateNode(nodeId: NodeId, leader: Boolean): ClusterChange
  def get():ClusterChange
  def reset
}
