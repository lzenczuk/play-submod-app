package com.github.lzenczuk.cn.crawler.flow.impl

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import akka.stream._
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerHttpResponse, CrawlerRequest, CrawlerResponse}

import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 21/11/16.
  */
object CrawlerFlows {

  case class UnsupportedMethodException(method: String) extends Exception

  sealed trait ValidatedCrawlerRequest
  case class InvalidCrawlerRequest(error: String, crawlerRequest: CrawlerRequest) extends ValidatedCrawlerRequest
  case class ValidCrawlerRequest(httpRequest: HttpRequest, crawlerRequest: CrawlerRequest) extends ValidatedCrawlerRequest


  implicit def crawlerMethodToHttpMethod(cm: CrawlerHttpMethod): HttpMethod = {
    cm match {
      case CrawlerHttpMethod.GET => HttpMethods.GET
      case m => throw UnsupportedMethodException(m.toString)
    }
  }

  def validationFlow:Flow[CrawlerRequest, ValidatedCrawlerRequest, NotUsed] = Flow[CrawlerRequest].map[ValidatedCrawlerRequest](crawlerRequestToValidatedCrawlerRequest(_))

  private def crawlerRequestToValidatedCrawlerRequest(cr:CrawlerRequest):ValidatedCrawlerRequest = {
    if(cr.url==null) return InvalidCrawlerRequest("Null url in request.", cr)
    if(cr.url.isEmpty) return InvalidCrawlerRequest("Empty url in request.", cr)

    var uri: Uri = null

    try {
      uri = Uri(cr.url)
    }catch {
      case iue:IllegalUriException => return InvalidCrawlerRequest(s"Invalid URL: ${iue.getMessage}", cr)
      case _:Throwable => return InvalidCrawlerRequest("Invalid URL format.", cr)
    }

    if(cr.method==null) return InvalidCrawlerRequest("Null method in request.", cr)

    var request: HttpRequest = null

    try{
      request = HttpRequest(cr.method, uri)
    }catch {
      case iae:IllegalArgumentException => return InvalidCrawlerRequest(s"Error creating http request: ${iae.getMessage}", cr)
      case _:Throwable => return InvalidCrawlerRequest("Unknown error creating http request.", cr)
    }
    ValidCrawlerRequest(request, cr)
  }

  def validationFailureFlow: Flow[ValidatedCrawlerRequest, CrawlerResponse, NotUsed] =
    Flow[ValidatedCrawlerRequest]
      .filter(_.isInstanceOf[InvalidCrawlerRequest])
      .map(_.asInstanceOf[InvalidCrawlerRequest])
      .map(ir => {
        CrawlerResponse(Some(ir.error), ir.crawlerRequest, List())
      })

  def validationSuccessFlow:Flow[ValidatedCrawlerRequest,(HttpRequest, CrawlerRequest), NotUsed] = Flow[ValidatedCrawlerRequest]
    .filter(_.isInstanceOf[ValidCrawlerRequest])
    .map(_.asInstanceOf[ValidCrawlerRequest])
    .map(vr => (vr.httpRequest, vr.crawlerRequest))

  def httpResponseFlow:Flow[(Try[HttpResponse], CrawlerRequest), CrawlerResponse, NotUsed] = Flow[(Try[HttpResponse], CrawlerRequest)].map{
    case (Failure(exception), cr) =>
      CrawlerResponse(Some(exception.getMessage), cr, List())
    case (Success(response), cr) =>
      CrawlerResponse(None, cr, List(CrawlerHttpResponse(response.status.intValue(), response.status.value)))
    case n =>
      CrawlerResponse(Some("What the hell is it?"), null, List())
  }

  def crawlerFlowWithoutRedirection(httpClientFlow: Flow[(HttpRequest, CrawlerRequest), (Try[HttpResponse], CrawlerRequest), NotUsed]): Flow[CrawlerRequest, CrawlerResponse, NotUsed] = {

    Flow.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>

        val input: FlowShape[CrawlerRequest, ValidatedCrawlerRequest] = builder.add(CrawlerFlows.validationFlow)
        val validationBCast: UniformFanOutShape[ValidatedCrawlerRequest, ValidatedCrawlerRequest] = builder.add(Broadcast[ValidatedCrawlerRequest](2))
        val output: UniformFanInShape[CrawlerResponse, CrawlerResponse] = builder.add(Merge[CrawlerResponse](2))

        input ~> validationBCast ~> CrawlerFlows.validationFailureFlow ~> output
        validationBCast ~> CrawlerFlows.validationSuccessFlow ~> httpClientFlow ~> CrawlerFlows.httpResponseFlow ~> output

        FlowShape(input.in, output.out)
    })
  }
}
