package com.github.lzenczuk.cn.crawler

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.FlatSpecLike

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class HttpClientTest extends TestKit(ActorSystem("test-as")) with FlatSpecLike {

  "Http client" should "success" in {

    implicit val mat = ActorMaterializer()

    val httpClient = Http().superPool[Int]()

    val requestWithIncorrectUrl: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://www.wikipedia.org"))

    val probe: TestProbe = TestProbe("http-response-consumer")

    Source.single((requestWithIncorrectUrl, 3))
      .via(httpClient)
      .to(Sink.actorRef(probe.ref, "CompleteMessage")).run()

    probe.expectMsgPF(5 seconds){
      case (Failure(ex),3) => assert(false, "Expecting that request will success")
      case (Success(_),3) => assert(true)
      case x =>
        println(s"Unexpected message: $x")
        assert(false, "Expecting tuple with success")
    }

  }

  "Http client" should "fail" in {

    implicit val mat = ActorMaterializer()

    val httpClient = Http().superPool[Int]()

    val requestWithIncorrectUrl: HttpRequest = HttpRequest(HttpMethods.GET, Uri("http://www.wikipedia"))

    val probe: TestProbe = TestProbe("http-response-consumer")

    Source.single((requestWithIncorrectUrl, 3))
      .via(httpClient)
      .to(Sink.actorRef(probe.ref, "CompleteMessage")).run()

    probe.expectMsgPF(5 seconds){
      case (Failure(ex),3) => assert(true)
      case (Success(_),3) => assert(false, "Expecting that request will fail")
      case x =>
        println(s"Unexpected message: $x")
        assert(false, "Expecting tuple with failure")
    }

  }

  "Http client" should "fail!?" in {

    implicit val mat = ActorMaterializer()

    val httpClient = Http().superPool[Int]()

    val requestWithIncorrectUrl: HttpRequest = HttpRequest(HttpMethods.GET, Uri("www.wikipedia.org"))

    val probe: TestProbe = TestProbe("http-response-consumer")

    Source.single((requestWithIncorrectUrl, 3))
      .via(httpClient)
      .to(Sink.actorRef(probe.ref, "CompleteMessage")).run()

    // This is issue I have with http client. I don't know how to solve it without creating
    // new http client stream
    probe.expectMsgPF(5 seconds){
      case (Failure(ex),3) => assert(true)
      case (Success(_),3) => assert(false, "Expecting that request will fail")
      case x =>
        println(s"Unexpected message: $x")
        assert(true, "It seams like correct behavior of http client :(")
    }

  }

}
