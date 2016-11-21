package com.github.lzenczuk.cn.crawler.actor

import javax.inject.Inject

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import com.github.lzenczuk.cn.crawler.domain.CrawlerRequest

import scala.util.Try

/**
  * Created by dev on 21/11/16.
  */

object CrawlerActor {

  val crawlerEventsTopic = "crawler-events-topic"
  val crawlerDiscoveryTopic = "crawler-events-topic"

  case object Ready
  case object StatusQuery
}

class CrawlerActor @Inject() (mediator:ActorRef, httpClientFlow:Flow[(HttpRequest, Long), (Try[HttpResponse], Long), NotUsed]) extends Actor with ActorLogging{
  import CrawlerActor._

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    mediator ! Subscribe(crawlerDiscoveryTopic, self)
    publishReadyEvent

  }

  def receive = {
    case StatusQuery => publishReadyEvent
    case cr:CrawlerRequest =>
      httpClientFlow
  }

  private def publishReadyEvent: Unit = {
    mediator ! Publish(crawlerEventsTopic, Ready)
  }

}
