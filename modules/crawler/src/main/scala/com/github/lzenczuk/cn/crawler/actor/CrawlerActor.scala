package com.github.lzenczuk.cn.crawler.actor

import javax.inject.Inject

import akka.actor.{ActorRef, FSM}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.github.lzenczuk.cn.crawler.actor.impl.{CrawlerData, CrawlerModel, EmptyModel}
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

  case class BusyWaitingForResponse(request: CrawlerRequest)

  object fsm {
    sealed trait State
    case object WaitingForRequest extends State
    case object WaitingForResponse extends State
  }
}

class CrawlerActor @Inject() (mediator:ActorRef, httpActor:ActorRef) extends FSM[CrawlerActor.fsm.State, CrawlerData]{
  import CrawlerActor._
  import CrawlerActor.fsm._

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    mediator ! Subscribe(crawlerDiscoveryTopic, self)
    publishReadyEvent
  }

  startWith(WaitingForRequest, EmptyModel)

  when(WaitingForRequest){
    case Event(cr:CrawlerRequest,EmptyModel) =>
      CrawlerModel(cr, sender) match {
        case Left((request, newModel)) =>
          httpActor ! request
          goto(WaitingForResponse) using newModel
        case Right(response) =>
          sender ! response
          stay()
      }
  }

  when(WaitingForResponse){
    case Event(thr:Try[HttpResponse], model:CrawlerModel) =>
      model.nextAction(thr) match {
        case Left((request, newModel)) =>
          httpActor ! request
          stay() using newModel
        case Right(response) =>
          model.initialRequestSender ! response
          goto(WaitingForRequest) using EmptyModel
      }
  }

  private def publishReadyEvent: Unit = {
    mediator ! Publish(crawlerEventsTopic, Ready)
  }

}
