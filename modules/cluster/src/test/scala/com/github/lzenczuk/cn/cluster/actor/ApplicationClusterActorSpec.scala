package com.github.lzenczuk.cn.cluster.actor

import akka.actor.{ActorSystem, Address, Terminated}
import akka.cluster.ClusterEvent.{CurrentClusterState, LeaderChanged, MemberEvent, MemberUp}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.github.lzenczuk.cn.cluster.actor.ApplicationClusterActor.SubscribeClusterStatus
import com.github.lzenczuk.cn.cluster.domain._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by dev on 15/11/16.
  */
class ApplicationClusterActorSpec extends TestKit(ActorSystem("ClusterNotificationActorSpec-system")) with FlatSpecLike with Matchers with MockitoSugar with ImplicitSender {

  "ClusterNotificationActor" should "subscribe to cluster events" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]
    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    verify(cluster).subscribe(actorRef, classOf[MemberEvent], classOf[LeaderChanged])
  }

  "ClusterNotificationActorSpec" should "should send cluster state to new subscriber" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    when(applicationCluster.get()).thenReturn(ClusterChange())

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    actorRef ! SubscribeClusterStatus

    expectMsgPF(){
      case cs:ClusterChange =>
        assert(cs.changes.size==0)
    }

    expectNoMsg()

    verify(applicationCluster).get()
  }

  "ClusterNotificationActor" should "should send cluster change after receiving init event to application cluster and propagate results to subscriber" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    when(applicationCluster.updateNode(NodeId("akka.tcp", "test-system", None, None), 1L, NodeState.Up))
      .thenReturn(ClusterChange(List(NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false))))
    when(applicationCluster.updateNode(NodeId("akka.tcp", "test-system", None, None), 2L, NodeState.Up))
      .thenReturn(ClusterChange(List(NodeChange("akka.tcp", "test-system", None, None, 2L, NodeState.Up, false))))
    when(applicationCluster.updateNode(NodeId("akka.tcp", "test-system", None, None), 3L, NodeState.Up))
      .thenReturn(ClusterChange(List(NodeChange("akka.tcp", "test-system", None, None, 3L, NodeState.Up, false))))
    when(applicationCluster.get()).thenReturn(ClusterChange(List(
      NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false),
      NodeChange("akka.tcp", "test-system", None, None, 2L, NodeState.Up, false),
      NodeChange("akka.tcp", "test-system", None, None, 3L, NodeState.Up, false)
    )))

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    val member1: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 1L, MemberStatus.up)
    val member2: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 2L, MemberStatus.up)
    val member3: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 3L, MemberStatus.up)

    val currentClusterState: CurrentClusterState = CurrentClusterState(scala.collection.immutable.SortedSet(member1, member2, member3))

    actorRef ! SubscribeClusterStatus

    expectMsgClass(classOf[ClusterChange])
    verify(applicationCluster, times(1)).get()

    actorRef ! currentClusterState

    expectMsgPF(){
      case cs:ClusterChange =>
        assert(cs.changes.size==3)
        assert(cs.changes(0)==NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false))
        assert(cs.changes(1)==NodeChange("akka.tcp", "test-system", None, None, 2L, NodeState.Up, false))
        assert(cs.changes(2)==NodeChange("akka.tcp", "test-system", None, None, 3L, NodeState.Up, false))
    }

    expectNoMsg()

    verify(applicationCluster, times(1)).get()
    verify(applicationCluster, times(1)).updateNode(NodeId("akka.tcp", "test-system", None, None), 1L, NodeState.Up)
    verify(applicationCluster, times(1)).updateNode(NodeId("akka.tcp", "test-system", None, None), 2L, NodeState.Up)
    verify(applicationCluster, times(1)).updateNode(NodeId("akka.tcp", "test-system", None, None), 3L, NodeState.Up)
  }

  "ClusterNotificationActor" should "should send change event after receiving member event" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    when(applicationCluster.updateNode(NodeId("akka.tcp", "test-system", None, None), 1L, NodeState.Up))
      .thenReturn(ClusterChange(List(NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false))))
    when(applicationCluster.get()).thenReturn(ClusterChange(List(
      NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false)
    )))

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    val member1: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 1L, MemberStatus.up)

    val memberUpEvent: MemberUp = MemberUp(member1)

    actorRef ! SubscribeClusterStatus

    expectMsgClass(classOf[ClusterChange])
    verify(applicationCluster, times(1)).get()

    actorRef ! memberUpEvent

    expectMsgPF(){
      case cs:ClusterChange =>
        assert(cs.changes.size==1)
        assert(cs.changes(0)==NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false))
    }

    expectNoMsg()

    verify(applicationCluster, times(1)).get()
    verify(applicationCluster, times(1)).updateNode(NodeId("akka.tcp", "test-system", None, None), 1L, NodeState.Up)
  }

  "ClusterNotificationActor" should "should change leader when receive LeaderChange message" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    when(applicationCluster.updateNode(NodeId("akka.tcp", "test-system", None, None), true))
      .thenReturn(ClusterChange(List(NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Unknown, true))))
    when(applicationCluster.get()).thenReturn(ClusterChange(List(
      NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Up, false)
    )))

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    val address: Address = Address("akka.tcp", "test-system")
    val leaderChanged: LeaderChanged = LeaderChanged(Some(address))

    actorRef ! SubscribeClusterStatus

    expectMsgClass(classOf[ClusterChange])
    verify(applicationCluster, times(1)).get()

    actorRef ! leaderChanged

    expectMsgPF(){
      case cs:ClusterChange =>
        assert(cs.changes.size==1)
        assert(cs.changes(0)==NodeChange("akka.tcp", "test-system", None, None, 1L, NodeState.Unknown, true))
    }

    expectNoMsg()

    verify(applicationCluster, times(1)).get()
    verify(applicationCluster, times(1)).updateNode(NodeId("akka.tcp", "test-system", None, None), true)
  }

  "ClusterNotificationActor" should "create cluster" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    val clusterSelfAddress = Address("akka.tcp", "cluster-test-system")
    when(cluster.selfAddress).thenReturn(clusterSelfAddress)

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    actorRef ! ApplicationClusterActor.CreateCluster

    verify(cluster).join(clusterSelfAddress)
  }

  "ClusterNotificationActor" should "join cluster" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    val otherNodeAddress = Address("akka.tcp", "cluster-test-system-2")

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    actorRef ! ApplicationClusterActor.JoinCluster(otherNodeAddress.protocol, otherNodeAddress.system, otherNodeAddress.host, otherNodeAddress.port)

    verify(cluster).join(otherNodeAddress)
  }

  "ClusterNotificationActor" should "leave cluster" in {

    val cluster = mock[Cluster]
    val applicationCluster = mock[ApplicationCluster]

    val clusterSelfAddress = Address("akka.tcp", "cluster-test-system")
    when(cluster.selfAddress).thenReturn(clusterSelfAddress)

    val actorRef = TestActorRef(new ApplicationClusterActor(cluster, applicationCluster))

    actorRef ! ApplicationClusterActor.LeaveCluster

    verify(cluster).leave(clusterSelfAddress)
  }
}
