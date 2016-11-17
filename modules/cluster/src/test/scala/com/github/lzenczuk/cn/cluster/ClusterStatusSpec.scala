package com.github.lzenczuk.cn.cluster

import akka.actor.Address
import akka.cluster.{MemberStatus, UniqueAddress}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by dev on 15/11/16.
  */
class ClusterStatusSpec extends FlatSpec with Matchers{

  "ClusterStatus" should "have None as leader and empty list of members" in {
    val clusterStatus: AppCluster = new AppCluster()

    assert(clusterStatus.leader.isEmpty)
    assert(clusterStatus.members.isEmpty)
  }

  "ClusterStatus" should "add new entry to members when it is empty and receive new entry" in {
    val clusterStatus: AppCluster = new AppCluster()

    val statuses = clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.joining)

    clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435")

    assert(clusterStatus.leader.isEmpty)
    assert(clusterStatus.members.size==1)
    assert(clusterStatus.members.contains("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").system=="test-system")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").host.contains("localhost"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").port.contains(2255))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").uid==345435L)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").leader==false)

    assert(statuses.size==1)
    assert(statuses(0).protocol=="akka.tcp")
    assert(statuses(0).system=="test-system")
    assert(statuses(0).host==Some("localhost"))
    assert(statuses(0).port==Some(2255))
    assert(statuses(0).uid==345435L)
    assert(statuses(0).nodeState == NodeState.Joining)
    assert(statuses(0).leader==false)
  }

  "ClusterStatus" should "add second entry when receive new status and already contains one" in {
    var clusterStatus: AppCluster = new AppCluster()

    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system2", "localhost2", 2256), 345436L), MemberStatus.joining)
    val statuses = clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.joining)

    assert(clusterStatus.leader.isEmpty)
    assert(clusterStatus.members.size==2)

    assert(clusterStatus.members.contains("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").system=="test-system")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").host.contains("localhost"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").port.contains(2255))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").uid==345435L)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").leader==false)

    assert(clusterStatus.members.contains("akka.tcptest-system2Some(localhost2)Some(2256)345436"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").system=="test-system2")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").host.contains("localhost2"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").port.contains(2256))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").uid==345436L)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").leader==false)

    assert(statuses.size==1)
    assert(statuses(0).protocol=="akka.tcp")
    assert(statuses(0).system=="test-system")
    assert(statuses(0).host.contains("localhost"))
    assert(statuses(0).port.contains(2255))
    assert(statuses(0).uid==345435L)
    assert(statuses(0).nodeState == NodeState.Joining)
    assert(statuses(0).leader==false)
  }

  "ClusterStatus" should "update existing entry" in {
    var clusterStatus: AppCluster = new AppCluster()

    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system2", "localhost2", 2256), 345436L), MemberStatus.joining)
    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.joining)

    val statuses = clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.up)

    assert(clusterStatus.leader.isEmpty)
    assert(clusterStatus.members.size==2)

    assert(clusterStatus.members.contains("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").system=="test-system")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").host==Some("localhost"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").port==Some(2255))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").uid==345435L)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").nodeState == NodeState.Up)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").leader==false)

    assert(clusterStatus.members.contains("akka.tcptest-system2Some(localhost2)Some(2256)345436"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").system=="test-system2")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").host==Some("localhost2"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").port==Some(2256))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").uid==345436L)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").leader==false)

    assert(statuses.size==1)
    assert(statuses(0).protocol=="akka.tcp")
    assert(statuses(0).system=="test-system")
    assert(statuses(0).host==Some("localhost"))
    assert(statuses(0).port==Some(2255))
    assert(statuses(0).uid==345435L)
    assert(statuses(0).nodeState == NodeState.Up)
    assert(statuses(0).leader==false)
  }

  "ClusterStatus" should "remove entry when member is down" in {
    val clusterStatus: AppCluster = new AppCluster()

    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system2", "localhost2", 2256), 345436L), MemberStatus.joining)
    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.joining)

    val statuses = clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.down)

    assert(clusterStatus.leader.isEmpty)
    assert(clusterStatus.members.size==1)

    assert(clusterStatus.members.contains("akka.tcptest-system2Some(localhost2)Some(2256)345436"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").system=="test-system2")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").host==Some("localhost2"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").port==Some(2256))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").uid==345436L)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").leader==false)

    assert(statuses.size==1)
    assert(statuses(0).protocol=="akka.tcp")
    assert(statuses(0).system=="test-system")
    assert(statuses(0).host==Some("localhost"))
    assert(statuses(0).port==Some(2255))
    assert(statuses(0).uid==345435L)
    assert(statuses(0).nodeState == NodeState.Down)
    assert(statuses(0).leader==false)
  }

  "ClusterStatus" should "set new leader" in {
    val clusterStatus: AppCluster = new AppCluster()

    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system2", "localhost2", 2256), 345436L), MemberStatus.joining)
    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.joining)

    val statuses = clusterStatus.setLeader(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L))

    assert(clusterStatus.leader==Some("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members.size==2)

    assert(clusterStatus.members.contains("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").system=="test-system")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").host==Some("localhost"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").port==Some(2255))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").uid==345435L)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").leader==true)

    assert(clusterStatus.members.contains("akka.tcptest-system2Some(localhost2)Some(2256)345436"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").system=="test-system2")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").host==Some("localhost2"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").port==Some(2256))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").uid==345436L)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").leader==false)

    assert(statuses.size==1)
    assert(statuses(0).protocol=="akka.tcp")
    assert(statuses(0).system=="test-system")
    assert(statuses(0).host==Some("localhost"))
    assert(statuses(0).port==Some(2255))
    assert(statuses(0).uid==345435L)
    assert(statuses(0).nodeState == NodeState.Joining)
    assert(statuses(0).leader==true)
  }

  "ClusterStatus" should "change leader" in {
    val clusterStatus: AppCluster = new AppCluster()

    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system2", "localhost2", 2256), 345436L), MemberStatus.joining)
    clusterStatus.updateStatus(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L), MemberStatus.joining)
    clusterStatus.setLeader(UniqueAddress(Address("akka.tcp", "test-system2", "localhost2", 2256), 345436L))

    val statuses = clusterStatus.setLeader(UniqueAddress(Address("akka.tcp", "test-system", "localhost", 2255), 345435L))

    assert(clusterStatus.leader==Some("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members.size==2)

    assert(clusterStatus.members.contains("akka.tcptest-systemSome(localhost)Some(2255)345435"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").system=="test-system")
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").host==Some("localhost"))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").port==Some(2255))
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").uid==345435L)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-systemSome(localhost)Some(2255)345435").leader==true)

    assert(clusterStatus.members.contains("akka.tcptest-system2Some(localhost2)Some(2256)345436"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").protocol=="akka.tcp")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").system=="test-system2")
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").host==Some("localhost2"))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").port==Some(2256))
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").uid==345436L)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").nodeState == NodeState.Joining)
    assert(clusterStatus.members("akka.tcptest-system2Some(localhost2)Some(2256)345436").leader==false)

    assert(statuses.size==2)
    assert(statuses(0).protocol=="akka.tcp")
    assert(statuses(0).system=="test-system")
    assert(statuses(0).host==Some("localhost"))
    assert(statuses(0).port==Some(2255))
    assert(statuses(0).uid==345435L)
    assert(statuses(0).nodeState == NodeState.Joining)
    assert(statuses(0).leader==true)
    assert(statuses(1).protocol=="akka.tcp")
    assert(statuses(1).system=="test-system2")
    assert(statuses(1).host==Some("localhost2"))
    assert(statuses(1).port==Some(2256))
    assert(statuses(1).uid==345436L)
    assert(statuses(1).nodeState == NodeState.Joining)
    assert(statuses(1).leader==false)
  }
}
