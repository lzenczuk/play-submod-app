package com.github.lzenczuk.cn.crawler.flow.impl

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import com.github.lzenczuk.cn.crawler.domain.{CrawlerHttpMethod, CrawlerRequest, CrawlerResponse}

import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 21/11/16.
  */
object CrawlerFlows {

  case class UnsupportedMethodException(method: String) extends Exception
  case class NullUrlException() extends Exception
  case class EmptyUrlException() extends Exception

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

  def validationSuccessFlow:Flow[ValidCrawlerRequest,(HttpRequest, CrawlerRequest), NotUsed] = Flow[ValidCrawlerRequest]
    .filter(_.isInstanceOf[ValidCrawlerRequest])
    .map(vr => (vr.httpRequest, vr.crawlerRequest))

  def httpResponseFlow:Flow[(Try[HttpResponse], CrawlerRequest), CrawlerResponse, NotUsed] = Flow[(Try[HttpResponse], CrawlerRequest)].map{
    case (Failure(response), cr) => CrawlerResponse(None, null, List())
    case (Success(response), cr) => CrawlerResponse(None, null, List())
  }

}
