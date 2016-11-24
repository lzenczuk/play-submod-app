package com.github.lzenczuk.cn.crawler.flow.impl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerRequest, CrawlerResponse}
import com.github.lzenczuk.cn.crawler.flow.Crawler
import org.scalatest.{BeforeAndAfter, FlatSpecLike, Matchers}

/**
  * Created by dev on 23/11/16.
  */
class CrawlerFactoryImplIntegrationSpec extends TestKit(ActorSystem("cfiis-as")) with FlatSpecLike with Matchers {

  "CrawlerFlow" should "fetch wikipedia page" in {

    val materializer: ActorMaterializer = ActorMaterializer()

    val crawlerFactoryImpl: CrawlerFactoryImpl = new CrawlerFactoryImpl(system, materializer)

    val dispatcherProbe: TestProbe = TestProbe("CrawlerDispatcherProbe")

    val crawlerFlow: Crawler = crawlerFactoryImpl.createFlow(dispatcherProbe.ref)

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    crawlerFlow.send(request)

    dispatcherProbe.expectMsgPF() {
      case cr: CrawlerResponse =>
        println(s"Got response: $cr")
        assert(cr.error.isEmpty)
        assert(cr.request == request)
        assert(cr.responses.size == 1)
      case x => println(s"Unknown message: $x")
    }

    val request2: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    crawlerFlow.send(request2)

    dispatcherProbe.expectMsgPF() {
      case cr: CrawlerResponse =>
        println(s"Got response: $cr")
        assert(cr.error.isEmpty)
        assert(cr.request == request)
        assert(cr.responses.size == 1)
      case x => println(s"Unknown message: $x")
    }
  }

  "CrawlerFlow" should "return crawler response with error when fetching non exisiting page" in {

    val materializer: ActorMaterializer = ActorMaterializer()

    val crawlerFactoryImpl: CrawlerFactoryImpl = new CrawlerFactoryImpl(system, materializer)

    val dispatcherProbe: TestProbe = TestProbe("CrawlerDispatcherProbe")

    val crawlerFlow: Crawler = crawlerFactoryImpl.createFlow(dispatcherProbe.ref)

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "lkhlkjhljhllkjh.com", false)
    crawlerFlow.send(request)

    dispatcherProbe.expectMsgPF() {
      case cr: CrawlerResponse =>
        println(s"Got response: $cr")
        assert(cr.error.isDefined)
        assert(cr.request == request)
        assert(cr.responses.isEmpty)
      case x =>
        assert(false, s"Expecting CrawlerResponse with error. Was $x")
    }

    val request2: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipadia.org", false)
    crawlerFlow.send(request2)

    dispatcherProbe.expectMsgPF() {
      case cr: CrawlerResponse =>
        println(s"Got response: $cr")
        assert(cr.error.isEmpty)
        assert(cr.request == request)
        assert(cr.responses.size == 1)
      case x => println(s"Unknown message: $x")
    }

  }

}
