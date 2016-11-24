package com.github.lzenczuk.cn.crawler.flow.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerRequest, CrawlerResponse}
import com.github.lzenczuk.cn.crawler.flow.impl.CrawlerFlows.{InvalidCrawlerRequest, ValidCrawlerRequest, ValidatedCrawlerRequest}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 21/11/16.
  */
class CrawlerFlowsSpec extends TestKit(ActorSystem("crawler-flows-spec-as")) with FlatSpecLike with Matchers{

  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  "validationFlow" should "produce valid request" in {

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.validationFlow).toMat(TestSink.probe[ValidatedCrawlerRequest])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)

    sub.request(1)
    pub.sendNext(request)
    sub.expectNextPF{
      case vr:ValidCrawlerRequest =>
        assert(vr.crawlerRequest==request)
        assert(vr.httpRequest!=null)
        assert(vr.httpRequest.uri.toString()=="http://www.wikipedia.org")
        assert(vr.httpRequest.method==HttpMethods.GET)
      case ir:InvalidCrawlerRequest => assert(false, "Should produce valid request")
    }

  }

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

  //----------------------------------------------------------------------------------------

  "validationFailureFlow" should "produce crawler response from invalid request" in {
    val (pub, sub) = TestSource.probe[ValidatedCrawlerRequest].via(CrawlerFlows.validationFailureFlow).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "", false)
    val invalidRequest = InvalidCrawlerRequest("Empty url in request.", request)

    sub.request(1)
    pub.sendNext(invalidRequest)
    sub.expectNextPF{
      case cr:CrawlerResponse =>
        assert(cr.error!=null)
        assert(cr.error.isDefined)
        assert(cr.error.contains("Empty url in request."))
        assert(cr.responses.isEmpty)
        assert(cr.request==request)
      case r => assert(false, s"Should produce CrawlerResponse. Was $r ")
    }
  }

  "validationFailureFlow" should "not produce any result when request is valid" in {
    val (pub, sub) = TestSource.probe[ValidatedCrawlerRequest].via(CrawlerFlows.validationFailureFlow).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "", false)
    val validRequest = ValidCrawlerRequest(HttpRequest(HttpMethods.GET, Uri("http://www.wikipedia.org")), request)

    sub.request(1)
    pub.sendNext(validRequest)
    sub.expectNoMsg(500 millis)
  }

  //----------------------------------------------------------------------------------------

  "validationSuccessFlow" should "not produce any response when revceive invalid request" in {
    val (pub, sub) = TestSource.probe[ValidatedCrawlerRequest].via(CrawlerFlows.validationSuccessFlow).toMat(TestSink.probe[(HttpRequest, CrawlerRequest)])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "", false)
    val invalidRequest = InvalidCrawlerRequest("Empty url in request.", request)

    sub.request(1)
    pub.sendNext(invalidRequest)
    sub.expectNoMsg(500 millis)
  }

  "validationSuccessFlow" should "not produce any result when request is valid" in {
    val (pub, sub) = TestSource.probe[ValidatedCrawlerRequest].via(CrawlerFlows.validationSuccessFlow).toMat(TestSink.probe[(HttpRequest, CrawlerRequest)])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    val validRequest = ValidCrawlerRequest(HttpRequest(HttpMethods.GET, Uri("http://www.wikipedia.org")), request)

    sub.request(1)
    pub.sendNext(validRequest)
    sub.expectNextPF{
      case (hr:HttpRequest, cr:CrawlerRequest) =>
        assert(hr!=null)
        assert(hr.method==HttpMethods.GET)
        assert(hr.uri==Uri("http://www.wikipedia.org"))
        assert(cr==request)
      case r => assert(false, s"Should produce HttpRequest, CrawlerResponse tuple. Was $r ")
    }
  }

  //----------------------------------------------------------------------------------------

  "httpResponseFlow" should "produce CrawlerResponse when http response is successful" in {
    val (pub, sub) = TestSource.probe[(Try[HttpResponse], CrawlerRequest)].via(CrawlerFlows.httpResponseFlow).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    val httpResponse: HttpResponse = HttpResponse(status = StatusCodes.OK)

    sub.request(1)
    pub.sendNext((Success(httpResponse), request))
    sub.expectNextPF{
      case cr:CrawlerResponse =>
        assert(cr.request==request)
        assert(cr.error.isEmpty)
        assert(cr.responses.size==1)
        assert(cr.responses(0).responseCode==200)
        assert(cr.responses(0).responseString=="200 OK")
      case r => assert(false, s"Should produce CrawlerResponse. Was $r")
    }
  }

  "httpResponseFlow" should "produce CrawlerResponse when http response is failure" in {
    val (pub, sub) = TestSource.probe[(Try[HttpResponse], CrawlerRequest)].via(CrawlerFlows.httpResponseFlow).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    val httpResponse: HttpResponse = HttpResponse(status = StatusCodes.OK)

    sub.request(1)
    pub.sendNext((Failure(new RuntimeException("Exception message")), request))
    sub.expectNextPF{
      case cr:CrawlerResponse =>
        assert(cr.request==request)
        assert(cr.error.contains("Exception message"))
        assert(cr.responses.isEmpty)
      case r => assert(false, s"Should produce CrawlerResponse. Was $r")
    }
  }

  //----------------------------------------------------------------------------------------

  "crawlerFlowWithoutRedirection" should "produce successful CrawlerResponse (hapy path)" in {

    val successfulHttpClientFlow: Flow[(HttpRequest, CrawlerRequest), (Try[HttpResponse], CrawlerRequest), NotUsed] = Flow[(HttpRequest, CrawlerRequest)].map(t => (Success(HttpResponse(StatusCodes.OK)), t._2))

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.crawlerFlowWithoutRedirection(successfulHttpClientFlow)).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    val httpResponse: HttpResponse = HttpResponse(status = StatusCodes.OK)

    sub.request(1)
    pub.sendNext(CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false))
    sub.expectNextPF{
      case cr:CrawlerResponse =>
        assert(cr.request==request)
        assert(cr.error.isEmpty)
        assert(cr.responses.size==1)
        assert(cr.responses(0).responseCode==200)
        assert(cr.responses(0).responseString=="200 OK")
      case r => assert(false, s"Should produce CrawlerResponse. Was $r")
    }
  }

  "crawlerFlowWithoutRedirection" should "produce error CrawlerResponse when http client fail" in {

    val failureHttpClientFlow: Flow[(HttpRequest, CrawlerRequest), (Try[HttpResponse], CrawlerRequest), NotUsed] = Flow[(HttpRequest, CrawlerRequest)].map(t => (Failure(new RuntimeException("Some exception")), t._2))

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.crawlerFlowWithoutRedirection(failureHttpClientFlow)).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false)
    val httpResponse: HttpResponse = HttpResponse(status = StatusCodes.OK)

    sub.request(1)
    pub.sendNext(CrawlerRequest(CrawlerHttpMethod.GET, "http://www.wikipedia.org", false))
    sub.expectNextPF{
      case cr:CrawlerResponse =>
        assert(cr.request==request)
        assert(cr.error.contains("Some exception"))
        assert(cr.responses.size==0)
      case r => assert(false, s"Should produce CrawlerResponse. Was $r")
    }
  }

  "crawlerFlowWithoutRedirection" should "produce error CrawlerResponse when receive invalid crawler request" in {

    val failureHttpClientFlow: Flow[(HttpRequest, CrawlerRequest), (Try[HttpResponse], CrawlerRequest), NotUsed] = Flow[(HttpRequest, CrawlerRequest)].map(t => (Failure(new RuntimeException("Some exception")), t._2))

    val (pub, sub) = TestSource.probe[CrawlerRequest].via(CrawlerFlows.crawlerFlowWithoutRedirection(failureHttpClientFlow)).toMat(TestSink.probe[CrawlerResponse])(Keep.both).run()

    val request: CrawlerRequest = CrawlerRequest(null, "http://www.wikipedia.org", false)
    val httpResponse: HttpResponse = HttpResponse(status = StatusCodes.OK)

    sub.request(1)
    pub.sendNext(CrawlerRequest(null, "http://www.wikipedia.org", false))
    sub.expectNextPF{
      case cr:CrawlerResponse =>
        assert(cr.request==request)
        assert(cr.error.contains("Null method in request."))
        assert(cr.responses.size==0)
      case r => assert(false, s"Should produce CrawlerResponse. Was $r")
    }
  }
}
