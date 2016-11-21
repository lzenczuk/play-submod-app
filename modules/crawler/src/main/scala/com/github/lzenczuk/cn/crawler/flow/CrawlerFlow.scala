package com.github.lzenczuk.cn.crawler.flow

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerRequest, CrawlerResponse}
import com.github.lzenczuk.cn.crawler.flow.impl.{InvalidCrawlerRequest, ValidCrawlerRequest, ValidatedCrawlerRequest}

import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 21/11/16.
  */
object CrawlerFlow {

  /*def create(httpClientFlow: Flow[(HttpRequest, CrawlerRequest), (Try[HttpResponse], CrawlerRequest), NotUsed]): Flow[CrawlerRequest, CrawlerResponse, NotUsed] = {

    GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>

        val httpFlow: Flow[ValidCrawlerRequest, CrawlerResponse, NotUsed] = validRequestFlow.via(httpClientFlow).via(httpResponseFlow)

        val vaidationBroadcast = builder.add(Broadcast[ValidatedCrawlerRequest](2))
        builder.add(Merge[CrawlerResponse](2))



        FlowShape(inFlow.in, httpOutFlow.out)

    }

  }*/

}
