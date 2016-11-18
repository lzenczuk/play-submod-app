package com.github.lzenczuk.cn.cluster.domain.impl

import com.github.lzenczuk.cn.cluster.domain.{ApplicationCluster, ClusterChange, NodeId, NodeState}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by dev on 18/11/16.
  */
class ApplicationClusterImplSpec extends FlatSpec with Matchers{

  def generateNodeId1: NodeId = {
    NodeId("akka.tcp", "cluster-system", Some("localhost1"), Some(2552))
  }

  def generateNodeId2: NodeId = {
    NodeId("akka.tcp", "cluster-system", Some("localhost2"), Some(2552))
  }

  "New ApplicationClusterImpl" should "return empty ClusterChange" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    val clusterState: ClusterChange = applicationCluster.get()

    assert(clusterState.changes.isEmpty)
  }

  "ApplicationClusterImpl" should "add new node" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)
    assert(clusterChange.changes.size==1)
    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost1"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==10L)
    assert(clusterChange.changes(0).nodeState==NodeState.Joining)
    assert(clusterChange.changes(0).leader==false)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==1)
    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Joining)
    assert(clusterState.changes(0).leader==false)
  }

  "ApplicationClusterImpl" should "change node status without leader state" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Up)
    assert(clusterChange.changes.size==1)
    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost1"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==10L)
    assert(clusterChange.changes(0).nodeState==NodeState.Up)
    assert(clusterChange.changes(0).leader==false)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==1)
    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Up)
    assert(clusterState.changes(0).leader==false)
  }

  "ApplicationClusterImpl" should "change node status without changing  leader state" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Up)
    assert(clusterChange.changes.size==1)
    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost1"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==10L)
    assert(clusterChange.changes(0).nodeState==NodeState.Up)
    assert(clusterChange.changes(0).leader==false)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==1)
    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Up)
    assert(clusterState.changes(0).leader==false)
  }

  "ApplicationClusterImpl" should "add second node" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId2, 11L, NodeState.Up)
    assert(clusterChange.changes.size==1)
    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost2"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==11L)
    assert(clusterChange.changes(0).nodeState==NodeState.Up)
    assert(clusterChange.changes(0).leader==false)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==2)

    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Joining)
    assert(clusterState.changes(0).leader==false)

    assert(clusterState.changes(1).protocol=="akka.tcp")
    assert(clusterState.changes(1).system=="cluster-system")
    assert(clusterState.changes(1).host.contains("localhost2"))
    assert(clusterState.changes(1).port.contains(2552))
    assert(clusterState.changes(1).uid==11L)
    assert(clusterState.changes(1).nodeState==NodeState.Up)
    assert(clusterState.changes(1).leader==false)
  }

  "ApplicationClusterImpl" should "change second node state" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)
    applicationCluster.updateNode(generateNodeId2, 11L, NodeState.Up)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId2, 11L, NodeState.Exiting)
    assert(clusterChange.changes.size==1)
    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost2"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==11L)
    assert(clusterChange.changes(0).nodeState==NodeState.Exiting)
    assert(clusterChange.changes(0).leader==false)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==2)

    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Joining)
    assert(clusterState.changes(0).leader==false)

    assert(clusterState.changes(1).protocol=="akka.tcp")
    assert(clusterState.changes(1).system=="cluster-system")
    assert(clusterState.changes(1).host.contains("localhost2"))
    assert(clusterState.changes(1).port.contains(2552))
    assert(clusterState.changes(1).uid==11L)
    assert(clusterState.changes(1).nodeState==NodeState.Exiting)
    assert(clusterState.changes(1).leader==false)
  }

  "ApplicationClusterImpl" should "let node to become leader" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)
    applicationCluster.updateNode(generateNodeId2, 11L, NodeState.Exiting)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId1, true)

    assert(clusterChange.changes.size==1)

    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost1"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==10L)
    assert(clusterChange.changes(0).nodeState==NodeState.Joining)
    assert(clusterChange.changes(0).leader==true)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==2)

    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Joining)
    assert(clusterState.changes(0).leader==true)

    assert(clusterState.changes(1).protocol=="akka.tcp")
    assert(clusterState.changes(1).system=="cluster-system")
    assert(clusterState.changes(1).host.contains("localhost2"))
    assert(clusterState.changes(1).port.contains(2552))
    assert(clusterState.changes(1).uid==11L)
    assert(clusterState.changes(1).nodeState==NodeState.Exiting)
    assert(clusterState.changes(1).leader==false)
  }

  "ApplicationClusterImpl" should "change leader" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)
    applicationCluster.updateNode(generateNodeId2, 11L, NodeState.Exiting)
    applicationCluster.updateNode(generateNodeId2, true)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId1, true)

    assert(clusterChange.changes.size==2)

    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost2"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==11L)
    assert(clusterChange.changes(0).nodeState==NodeState.Exiting)
    assert(clusterChange.changes(0).leader==false)

    assert(clusterChange.changes(1).protocol=="akka.tcp")
    assert(clusterChange.changes(1).system=="cluster-system")
    assert(clusterChange.changes(1).host.contains("localhost1"))
    assert(clusterChange.changes(1).port.contains(2552))
    assert(clusterChange.changes(1).uid==10L)
    assert(clusterChange.changes(1).nodeState==NodeState.Joining)
    assert(clusterChange.changes(1).leader==true)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==2)

    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Joining)
    assert(clusterState.changes(0).leader==true)

    assert(clusterState.changes(1).protocol=="akka.tcp")
    assert(clusterState.changes(1).system=="cluster-system")
    assert(clusterState.changes(1).host.contains("localhost2"))
    assert(clusterState.changes(1).port.contains(2552))
    assert(clusterState.changes(1).uid==11L)
    assert(clusterState.changes(1).nodeState==NodeState.Exiting)
    assert(clusterState.changes(1).leader==false)
  }

  "ApplicationClusterImpl" should "let node to become non leader node" in {
    val applicationCluster: ApplicationCluster = new ApplicationClusterImpl

    applicationCluster.updateNode(generateNodeId1, 10L, NodeState.Joining)
    applicationCluster.updateNode(generateNodeId2, 11L, NodeState.Up)
    applicationCluster.updateNode(generateNodeId2, true)

    val clusterChange: ClusterChange = applicationCluster.updateNode(generateNodeId2, false)
    assert(clusterChange.changes.size==1)
    assert(clusterChange.changes(0).protocol=="akka.tcp")
    assert(clusterChange.changes(0).system=="cluster-system")
    assert(clusterChange.changes(0).host.contains("localhost2"))
    assert(clusterChange.changes(0).port.contains(2552))
    assert(clusterChange.changes(0).uid==11L)
    assert(clusterChange.changes(0).nodeState==NodeState.Up)
    assert(clusterChange.changes(0).leader==false)

    val clusterState: ClusterChange = applicationCluster.get()
    assert(clusterState.changes.size==2)

    assert(clusterState.changes(0).protocol=="akka.tcp")
    assert(clusterState.changes(0).system=="cluster-system")
    assert(clusterState.changes(0).host.contains("localhost1"))
    assert(clusterState.changes(0).port.contains(2552))
    assert(clusterState.changes(0).uid==10L)
    assert(clusterState.changes(0).nodeState==NodeState.Joining)
    assert(clusterState.changes(0).leader==false)

    assert(clusterState.changes(1).protocol=="akka.tcp")
    assert(clusterState.changes(1).system=="cluster-system")
    assert(clusterState.changes(1).host.contains("localhost2"))
    assert(clusterState.changes(1).port.contains(2552))
    assert(clusterState.changes(1).uid==11L)
    assert(clusterState.changes(1).nodeState==NodeState.Up)
    assert(clusterState.changes(1).leader==false)
  }
}
