package com.github.lzenczuk.cn.crawler.actor

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerRequest, CrawlerResponse}
import org.mockserver.client.server.MockServerClient
import org.mockserver.mockserver.MockServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest._
import org.mockserver.model.HttpResponse._
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
  * Created by dev on 25/11/16.
  */
class CrawlerActorIntegrationTest extends TestKit(ActorSystem("CAIT-as")) with FlatSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll{

  var mockServerClient:MockServerClient = null
  var mockServer: MockServer = null

  override protected def beforeAll(): Unit = {

    mockServer = new MockServer(8899)
    mockServerClient = new MockServerClient("localhost", 8899)

    mockServerClient.when(request().withPath("/result")).respond(response("result").withStatusCode(200))

    mockServerClient.when(request().withPath("/redirect1")).respond(response("redirect1").withStatusCode(301).withHeader(new Header("Location", "/result")))
    mockServerClient.when(request().withPath("/redirect2")).respond(response("redirect2").withStatusCode(301).withHeader(new Header("Location", "/redirect1")))
    mockServerClient.when(request().withPath("/redirect3")).respond(response("redirect3").withStatusCode(301).withHeader(new Header("Location", "/redirect2")))

    mockServerClient.when(request().withPath("/aredirect1")).respond(response("aredirect1").withStatusCode(301).withHeader(new Header("Location", "http://localhost:8899/result")))
    mockServerClient.when(request().withPath("/aredirect2")).respond(response("aredirect2").withStatusCode(301).withHeader(new Header("Location", "http://localhost:8899/aredirect1")))
    mockServerClient.when(request().withPath("/aredirect3")).respond(response("aredirect3").withStatusCode(301).withHeader(new Header("Location", "http://localhost:8899/aredirect2")))

    mockServerClient.when(request().withPath("/notFound")).respond(response("notFound").withStatusCode(404))

    mockServerClient.when(request().withPath("/delay2s")).respond(response("delay2s").withStatusCode(200).withDelay(TimeUnit.SECONDS, 2))
  }

  override protected def afterAll(): Unit = {
    if(mockServerClient!=null){
      mockServerClient.stop()
    }

    if(mockServer!=null){
      mockServer.stop()
    }
  }

  "CrawlerActor" should "fetch page" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/result", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==1)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }

  "CrawlerActor" should "fetch page with single absolute redirection" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/aredirect1", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==2)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/aredirect1")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(1).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(1).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(1).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }

  "CrawlerActor" should "fetch page with two absolute redirections" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/aredirect2", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==3)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/aredirect2")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(1).crawlerHttpRequest.url=="http://localhost:8899/aredirect1")
        assert(cr.steps(1).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(1).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(2).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(2).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(2).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }

  "CrawlerActor" should "fetch page with three absolute redirections" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/aredirect3", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==4)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/aredirect3")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(1).crawlerHttpRequest.url=="http://localhost:8899/aredirect2")
        assert(cr.steps(1).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(1).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(2).crawlerHttpRequest.url=="http://localhost:8899/aredirect1")
        assert(cr.steps(2).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(2).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(3).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(3).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(3).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }

  "CrawlerActor" should "fetch page with single relative redirection" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/redirect1", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==2)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/redirect1")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(1).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(1).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(1).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }

  "CrawlerActor" should "fetch page with two relative redirections" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/redirect2", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==3)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/redirect2")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(1).crawlerHttpRequest.url=="http://localhost:8899/redirect1")
        assert(cr.steps(1).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(1).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(2).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(2).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(2).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }

  "CrawlerActor" should "fetch page with three relative redirections" in {
    val httpActor: TestActorRef[HttpActor] = TestActorRef[HttpActor]
    val mediatorProbe = TestProbe()
    val crawlerActor: TestActorRef[CrawlerActor] = TestActorRef(new CrawlerActor(mediatorProbe.ref, httpActor))

    crawlerActor ! CrawlerRequest(CrawlerHttpMethod.GET, "http://localhost:8899/redirect3", false)

    expectMsgPF(){
      case cr:CrawlerResponse =>
        assert(cr.error.isEmpty)
        assert(cr.steps.size==4)
        assert(cr.steps(0).crawlerHttpRequest.url=="http://localhost:8899/redirect3")
        assert(cr.steps(0).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(0).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(1).crawlerHttpRequest.url=="http://localhost:8899/redirect2")
        assert(cr.steps(1).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(1).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(2).crawlerHttpRequest.url=="http://localhost:8899/redirect1")
        assert(cr.steps(2).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(2).crawlerHttpResponseOption.get.responseCode==301)
        assert(cr.steps(3).crawlerHttpRequest.url=="http://localhost:8899/result")
        assert(cr.steps(3).crawlerHttpResponseOption.isDefined)
        assert(cr.steps(3).crawlerHttpResponseOption.get.responseCode==200)
      case x => assert(false, s"Expect CrawlerResponse, was $x")
    }
  }
}
