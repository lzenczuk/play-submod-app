package com.github.lzenczuk.cn.crawler.flow.impl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerRequest}
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by dev on 21/11/16.
  */
class CrawlerFlowsSpec extends TestKit(ActorSystem("crawler-flows-spec-as")) with FlatSpecLike with Matchers{

  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  "validationFlow" should "produce invalid request when url is null" in {

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.validationFlow).toMat(TestSink.probe[ValidatedCrawlerRequest])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, null, false)

    sub.request(1)
    pub.sendNext(request)
    sub.expectNext(InvalidCrawlerRequest("Null url in request.", request))
  }

  "validationFlow" should "produce invalid request when url empty" in {

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.validationFlow).toMat(TestSink.probe[ValidatedCrawlerRequest])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "", false)

    sub.request(1)
    pub.sendNext(request)
    sub.expectNext(InvalidCrawlerRequest("Empty url in request.", request))
  }

  "validationFlow" should "produce invalid request when url is in incorrect format" in {

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.validationFlow).toMat(TestSink.probe[ValidatedCrawlerRequest])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "htt://test.com", false)

    sub.request(1)
    pub.sendNext(request)
    sub.expectNext(InvalidCrawlerRequest("Error creating http request: `uri` must have scheme \"http\", \"https\" or no scheme", request))
  }

  "validationFlow" should "produce invalid request don't have http method" in {

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.validationFlow).toMat(TestSink.probe[ValidatedCrawlerRequest])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(null, "http://test.com", false)

    sub.request(1)
    pub.sendNext(request)
    sub.expectNext(InvalidCrawlerRequest("Null method in request.", request))
  }
}
