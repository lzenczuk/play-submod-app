package com.github.lzenczuk.cn.crawler.actor

import akka.NotUsed
import akka.actor.ActorSystem
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.github.lzenczuk.cn.crawler.domain.CrawlerRequest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, Matchers}

import scala.util.Try

/**
  * Created by dev on 21/11/16.
  */
class CrawlerActorSpec extends TestKit(ActorSystem("crawler-actor-spec-as")) with FlatSpecLike with Matchers with MockitoSugar{

  "CrawlerActor" should "subscribe to discovery topic and publish ready message after start" in {

    val mediatorProbe = TestProbe()

    val crawler: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, null))

    mediatorProbe.expectMsg(Subscribe(CrawlerActor.crawlerDiscoveryTopic, crawler))
    mediatorProbe.expectMsg(Publish(CrawlerActor.crawlerEventsTopic, CrawlerActor.Ready))
  }

  "CrawlerActor" should "publish ready message after receiving status query" in {

    val mediatorProbe = TestProbe()

    val crawler: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, null))

    mediatorProbe.expectMsg(Subscribe(CrawlerActor.crawlerDiscoveryTopic, crawler))
    mediatorProbe.expectMsg(Publish(CrawlerActor.crawlerEventsTopic, CrawlerActor.Ready))

    crawler ! CrawlerActor.StatusQuery

    mediatorProbe.expectMsg(Publish(CrawlerActor.crawlerEventsTopic, CrawlerActor.Ready))
  }
}
