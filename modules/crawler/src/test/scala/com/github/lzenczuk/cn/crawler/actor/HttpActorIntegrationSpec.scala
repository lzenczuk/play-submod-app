package com.github.lzenczuk.cn.crawler.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, PoisonPill}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.github.lzenczuk.cn.crawler.actor.HttpActor.{BusyWaitingForResponse, HttpActorException}
import org.mockserver.client.server.MockServerClient
import org.mockserver.mockserver.MockServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by dev on 24/11/16.
  */
class HttpActorIntegrationSpec extends TestKit(ActorSystem("HAS-as")) with FlatSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {

  var mockServerClient:MockServerClient = null
  var mockServer: MockServer = null

  override protected def beforeAll(): Unit = {

    mockServer = new MockServer(8899)
    mockServerClient = new MockServerClient("localhost", 8899)

    mockServerClient.when(request().withPath("/correct1")).respond(response("correct1").withStatusCode(200))
    mockServerClient.when(request().withPath("/correct2")).respond(response("correct2").withStatusCode(200))
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

  implicit val materializer = ActorMaterializer()

  "HttpActor" should "fetch two web pages" in {

    val httpActorRef: TestActorRef[HttpActor] = TestActorRef[HttpActor]

    val requestWithCorrectUrl1: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/correct1"))
    val requestWithCorrectUrl2: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/correct2"))

    httpActorRef ! requestWithCorrectUrl1

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(response.status.intValue()==200)
        assert(!response.entity.isKnownEmpty())

        val futureBody: Future[String] = response.entity.dataBytes.map(_.utf8String).runWith(Sink.head[String])
        val body: String = Await.result(futureBody, 1 second)
        assert(body=="correct1")
      case Failure(ex) =>
        assert(false, s"Expecting successful response, was exception: ${ex.getMessage}")
      case x =>
        assert(false, s"Unknown response: $x")
    }

    httpActorRef ! requestWithCorrectUrl2

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(response.status.intValue()==200)
        assert(!response.entity.isKnownEmpty())

        val futureBody: Future[String] = response.entity.dataBytes.map(_.utf8String).runWith(Sink.head[String])
        val body: String = Await.result(futureBody, 1 second)
        assert(body=="correct2")
      case Failure(ex) =>
        assert(false, s"Expecting successful response, was exception: ${ex.getMessage}")
      case x =>
        assert(false, s"Unknown response: $x")
    }
  }

  "HttpActor" should "send back failure for first page and fetch second" in {

    val httpActorRef: TestActorRef[HttpActor] = TestActorRef[HttpActor]

    val requestWithNotExisitingHost: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://lklklkhkhkhk.org"))
    val requestWithCorrectUrl1: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/correct1"))

    httpActorRef ! requestWithNotExisitingHost

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(false, s"Expecting failure but was: $response")
      case Failure(ex) =>
      case x =>
        assert(false, s"Unknown response: $x")
    }

    httpActorRef ! requestWithCorrectUrl1

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(response.status.intValue()==200)
        assert(!response.entity.isKnownEmpty())

        val futureBody: Future[String] = response.entity.dataBytes.map(_.utf8String).runWith(Sink.head[String])
        val body: String = Await.result(futureBody, 1 second)
        assert(body=="correct1")
      case Failure(ex) =>
        assert(false, s"Expecting successful response, was exception: ${ex.getMessage}")
      case x =>
        assert(false, s"Unknown response: $x")
    }
  }

  "HttpActor" should "be able to make requests after request causing exception in akka http client" in {

    val httpActorRef: TestActorRef[HttpActor] = TestActorRef[HttpActor]

    val requestWithCorrectUrl1: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/correct1"))
    val requestWithUrlCausingHttpClientException: HttpRequest = HttpRequest(HttpMethods.GET, Uri("www.wikipedia.org"))
    val requestWithCorrectUrl2: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/correct2"))

    println("First correct request")
    httpActorRef ! requestWithCorrectUrl1

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(response.status.intValue()==200)
        assert(!response.entity.isKnownEmpty())

        val futureBody: Future[String] = response.entity.dataBytes.map(_.utf8String).runWith(Sink.head[String])
        val body: String = Await.result(futureBody, 1 second)
        assert(body=="correct1")
      case Failure(ex) =>
        assert(false, s"Expecting successful response, was exception: ${ex.getMessage}")
      case x =>
        assert(false, s"Unknown response: $x")
    }

    println("Incorrect request")
    httpActorRef ! requestWithUrlCausingHttpClientException

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(false, s"Expecting failure but was: $response")
      case Failure(ex) =>
      case x =>
        assert(false, s"Unknown response: $x")
    }

    println("Second correct request")
    httpActorRef ! requestWithCorrectUrl2

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(response.status.intValue()==200)
        assert(!response.entity.isKnownEmpty())

        val futureBody: Future[String] = response.entity.dataBytes.map(_.utf8String).runWith(Sink.head[String])
        val body: String = Await.result(futureBody, 1 second)
        assert(body=="correct2")
      case Failure(ex) =>
        assert(false, s"Expecting successful response, was exception: ${ex.getMessage}")
      case x =>
        assert(false, s"Unknown response: $x")
    }
  }

  "HttpActor" should "response with busy message when receive new request before fetching previous" in {

    val httpActorRef: TestActorRef[HttpActor] = TestActorRef[HttpActor]

    val requestWithDelay2s: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/delay2s"))
    val requestWithCorrectUrl2: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/correct2"))

    httpActorRef ! requestWithDelay2s
    httpActorRef ! requestWithCorrectUrl2

    expectMsg(BusyWaitingForResponse(requestWithCorrectUrl2))

    expectMsgPF(5 seconds) {
      case Success(response: HttpResponse) =>
        assert(response.status.intValue()==200)
        assert(!response.entity.isKnownEmpty())

        val futureBody: Future[String] = response.entity.dataBytes.map(_.utf8String).runWith(Sink.head[String])
        val body: String = Await.result(futureBody, 1 second)
        assert(body=="delay2s")
      case Failure(ex) =>
        assert(false, s"Expecting successful response, was exception: ${ex.getMessage}")
      case x =>
        assert(false, s"Unknown response: $x")
    }
  }

  "HttpActor" should "finish processing before stopping" in {

    val httpActorRef: TestActorRef[HttpActor] = TestActorRef[HttpActor]

    val requestWithDelay2s: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://localhost:8899/delay2s"))

    httpActorRef ! requestWithDelay2s
    httpActorRef ! PoisonPill

    expectMsg(Failure(HttpActorException("Http actor stopped.")))

    expectNoMsg()
  }
}
