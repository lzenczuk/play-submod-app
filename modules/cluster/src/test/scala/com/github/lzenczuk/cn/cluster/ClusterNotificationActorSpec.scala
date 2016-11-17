package com.github.lzenczuk.cn.cluster

import akka.actor.ActorSystem
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberEvent}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.github.lzenczuk.cn.cluster.ClusterNotificationActor.SubscribeClusterStatus
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by dev on 15/11/16.
  */
class ClusterNotificationActorSpec extends TestKit(ActorSystem("ClusterNotificationActorSpec-system")) with FlatSpecLike with Matchers with MockitoSugar with ImplicitSender {

  "ClusterNotificationActorSpec" should "subscribe to cluster events" in {

    val cluster = mock[Cluster]
    val actorRef = TestActorRef(new ClusterNotificationActor(cluster))

    verify(cluster).subscribe(actorRef, classOf[MemberEvent])
  }

  "ClusterNotificationActorSpec" should "should send cluster state to new subscriber" in {

    val cluster = mock[Cluster]
    val actorRef = TestActorRef(new ClusterNotificationActor(cluster))

    val member1: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 1L, MemberStatus.up)
    val member2: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 2L, MemberStatus.up)
    val member3: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 3L, MemberStatus.up)

    val currentClusterState: CurrentClusterState = CurrentClusterState(scala.collection.immutable.SortedSet(member1, member2, member3))

    actorRef ! currentClusterState

    actorRef ! SubscribeClusterStatus

    expectMsgPF(){
      case cs:AppCluster =>
        assert(cs.leader.isEmpty)
        assert(cs.members.size==3)
        assert(cs.members.contains("akka.tcptest-systemNoneNone1"))
        assert(cs.members.contains("akka.tcptest-systemNoneNone2"))
        assert(cs.members.contains("akka.tcptest-systemNoneNone3"))
    }

    expectNoMsg()
  }

  "ClusterNotificationActorSpec" should "should send empty cluster state to subscriber when not receive CurrentClusterState yet" in {

    val cluster = mock[Cluster]
    val actorRef = TestActorRef(new ClusterNotificationActor(cluster))

    val member1: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 1L, MemberStatus.up)
    val member2: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 2L, MemberStatus.up)
    val member3: Member = akka.cluster.createTestClusterMember("akka.tcp", "test-system", 3L, MemberStatus.up)

    val currentClusterState: CurrentClusterState = CurrentClusterState(scala.collection.immutable.SortedSet(member1, member2, member3))

    actorRef ! SubscribeClusterStatus

    expectMsgPF(){
      case cs:AppCluster =>
        assert(cs.leader.isEmpty)
        assert(cs.members.isEmpty)
    }

    expectNoMsg()

    actorRef ! currentClusterState

    expectMsgClass(classOf[AppClusterNodeChange])
    expectMsgClass(classOf[AppClusterNodeChange])
    expectMsgClass(classOf[AppClusterNodeChange])
  }

}
