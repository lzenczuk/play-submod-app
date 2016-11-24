package com.github.lzenczuk.cn.crawler.flow

import akka.actor.ActorRef
import com.github.lzenczuk.cn.crawler.domain.CrawlerRequest

/**
  * Created by dev on 23/11/16.
  */
trait Crawler {
  def kill
  def send(crawlerRequest: CrawlerRequest)
}

trait CrawlerFactory{
  def createFlow(responseReceiver:ActorRef):Crawler
}
