package com.github.lzenczuk.cn.crawler.flow.impl

import javax.inject.Inject

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream._
import com.github.lzenczuk.cn.crawler.domain.{CrawlerRequest, CrawlerResponse}
import com.github.lzenczuk.cn.crawler.flow.{Crawler, CrawlerFactory}

/**
  * Created by dev on 23/11/16.
  */
class CrawlerFactoryImpl @Inject()(system: ActorSystem, materializer: Materializer) extends CrawlerFactory{

  case object CrawlerImpl {
    def apply(t:(ActorRef,KillSwitch)):CrawlerImpl = CrawlerImpl(t._2, t._1)
  }

  case class CrawlerImpl(private val killSwitch:KillSwitch, private val sourceActor:ActorRef) extends Crawler{
    def kill = killSwitch.shutdown()
    def send(crawlerRequest: CrawlerRequest) = sourceActor ! crawlerRequest
  }

  override def createFlow(responseReceiver:ActorRef):Crawler = {

    val httpClientFlow = Http(system).superPool[CrawlerRequest]()(materializer)
    val flow: Flow[CrawlerRequest, CrawlerResponse, NotUsed] = CrawlerFlows.crawlerFlowWithoutRedirection(httpClientFlow)

    CrawlerImpl(Source.actorRef[CrawlerRequest](1, OverflowStrategy.fail)
      .viaMat(KillSwitches.single)(Keep.both)
      .via(flow)
      .to(Sink.actorRef[CrawlerResponse](responseReceiver, "Termination message"))
      .run()(materializer))
  }

}
