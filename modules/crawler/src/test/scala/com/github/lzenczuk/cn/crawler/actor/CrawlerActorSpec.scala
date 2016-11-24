package com.github.lzenczuk.cn.crawler.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.github.lzenczuk.cn.crawler.flow.{Crawler, CrawlerFactory}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, Matchers}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

/**
  * Created by dev on 21/11/16.
  */
class CrawlerActorSpec extends TestKit(ActorSystem("crawler-actor-spec-as")) with FlatSpecLike with Matchers with MockitoSugar{

  "CrawlerActor" should "subscribe to discovery topic and publish ready message after start" in {

    val mediatorProbe = TestProbe()
    val crawlerFactoryMock: CrawlerFactory = mock[CrawlerFactory]
    when(crawlerFactoryMock.createFlow(any[ActorRef])).thenReturn(mock[Crawler])

    val crawler: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, crawlerFactoryMock))

    mediatorProbe.expectMsg(Subscribe(CrawlerActor.crawlerDiscoveryTopic, crawler))
    mediatorProbe.expectMsg(Publish(CrawlerActor.crawlerEventsTopic, CrawlerActor.Ready))
  }

  "CrawlerActor" should "publish ready message after receiving status query" in {

    val mediatorProbe = TestProbe()
    val crawlerFactoryMock: CrawlerFactory = mock[CrawlerFactory]
    when(crawlerFactoryMock.createFlow(any[ActorRef])).thenReturn(mock[Crawler])

    val crawler: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, crawlerFactoryMock))

    mediatorProbe.expectMsg(Subscribe(CrawlerActor.crawlerDiscoveryTopic, crawler))
    mediatorProbe.expectMsg(Publish(CrawlerActor.crawlerEventsTopic, CrawlerActor.Ready))

    crawler ! CrawlerActor.StatusQuery

    mediatorProbe.expectMsg(Publish(CrawlerActor.crawlerEventsTopic, CrawlerActor.Ready))
  }
}
