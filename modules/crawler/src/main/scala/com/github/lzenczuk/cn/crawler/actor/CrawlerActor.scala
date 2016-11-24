package com.github.lzenczuk.cn.crawler.actor

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import com.github.lzenczuk.cn.crawler.domain.{CrawlerRequest, CrawlerResponse}
import com.github.lzenczuk.cn.crawler.flow.CrawlerFactory

/**
  * Created by dev on 21/11/16.
  */

object CrawlerActor {

  val crawlerEventsTopic = "crawler-events-topic"
  val crawlerDiscoveryTopic = "crawler-events-topic"

  case object Ready
  case object StatusQuery
}

class CrawlerActor @Inject() (mediator:ActorRef, crawlerFlowFactory: CrawlerFactory) extends Actor with ActorLogging{
  import CrawlerActor._

  val crawlerFlow = crawlerFlowFactory.createFlow(self)

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    mediator ! Subscribe(crawlerDiscoveryTopic, self)
    publishReadyEvent
  }

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    crawlerFlow.kill
  }


  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    crawlerFlow.kill
  }

  def receive = {
    case StatusQuery => publishReadyEvent
    case cr:CrawlerRequest => crawlerFlow.send(cr)
    case cr:CrawlerResponse => println()
  }

  private def publishReadyEvent: Unit = {
    mediator ! Publish(crawlerEventsTopic, Ready)
  }

}
