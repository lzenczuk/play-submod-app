package com.github.lzenczuk.cn.crawler.actor.impl

import akka.actor.ActorRef
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import com.github.lzenczuk.cn.crawler.actor.impl.CrawlerModel.UnsupportedMethodException
import com.github.lzenczuk.cn.crawler.domain._

import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 25/11/16.
  */

sealed trait CrawlerData
case object EmptyModel extends CrawlerData

object CrawlerModel{

  case class UnsupportedMethodException(message:String) extends Exception

  implicit def crawlerMethodToHttpMethod(cm: CrawlerHttpMethod): HttpMethod = {
    cm match {
      case CrawlerHttpMethod.GET => HttpMethods.GET
      case m => throw UnsupportedMethodException(m.toString)
    }
  }

  def apply(crawlerRequest: CrawlerRequest, sender:ActorRef): Either[(HttpRequest, CrawlerModel), CrawlerResponse] = {
    if(crawlerRequest.url==null) return Right(CrawlerResponse(Some("Null url in request."), crawlerRequest))
    if(crawlerRequest.url.isEmpty) return Right(CrawlerResponse(Some("Empty url in request."), crawlerRequest))

    var uri: Uri = null

    try {
      uri = Uri(crawlerRequest.url)
    }catch {
      case iue:IllegalUriException => return Right(CrawlerResponse(Some(s"Invalid URL: ${iue.getMessage}"), crawlerRequest))
      case _:Throwable => return Right(CrawlerResponse(Some("Invalid URL format."), crawlerRequest))
    }

    if(crawlerRequest.method==null) return Right(CrawlerResponse(Some("Null method in request."), crawlerRequest))

    var request: HttpRequest = null

    try{
      request = HttpRequest(crawlerRequest.method, uri)
    }catch {
      case iae:IllegalArgumentException => return Right(CrawlerResponse(Some(s"Error creating http request: ${iae.getMessage}"), crawlerRequest))
      case _:Throwable => return Right(CrawlerResponse(Some("Unknown error creating http request."), crawlerRequest))
    }
    Left((request, new CrawlerModel(crawlerRequest, sender, List(request), List())))
  }
}

class CrawlerModel(private val crawlerRequest: CrawlerRequest,
                   val initialRequestSender:ActorRef,
                   private val requests:List[HttpRequest],
                   private val responses:List[HttpResponse]) extends CrawlerData{
  def nextAction(httpResponse: Try[HttpResponse]): Either[(HttpRequest, CrawlerModel), CrawlerResponse] = {

    httpResponse  match {
      case Success(response) if response.status.isRedirection => redirect(response)
      case Success(response) => success(response)
      case Failure(ex) => failure(ex.getMessage)
    }
  }

  protected def success(response:HttpResponse): Either[(HttpRequest, CrawlerModel), CrawlerResponse] = {
    Right(CrawlerResponse(None, crawlerRequest, generateSteps(Some(response))))
  }

  protected def failure(errorMassage:String): Either[(HttpRequest, CrawlerModel), CrawlerResponse] = {
    Right(CrawlerResponse(Some(errorMassage), crawlerRequest, generateSteps(None)))
  }

  protected def redirect(response:HttpResponse): Either[(HttpRequest, CrawlerModel), CrawlerResponse] = {
    val optionalRequest:Option[HttpRequest] = response.headers.find(_.lowercaseName()=="location").map(_.value()).map(redirectionAddress =>
      if(redirectionAddress.startsWith("http://") || redirectionAddress.startsWith("https://")){
        HttpRequest(HttpMethods.GET, Uri(redirectionAddress))
      }else{
        HttpRequest(HttpMethods.GET, requests.last.uri.withPath(Path(redirectionAddress)))
      }
    )

    optionalRequest
      .map(request => Left((request, new CrawlerModel(crawlerRequest, initialRequestSender, requests :+ request, responses :+ response))))
      .getOrElse{
        Right(CrawlerResponse(Some("Redirect response but without location"), crawlerRequest, generateSteps(Some(response))))
      }
  }

  protected def httpResponseToCrawlerHttpResponse(httpResponse: HttpResponse):CrawlerHttpResponse = {
    CrawlerHttpResponse(httpResponse.status.intValue(), httpResponse.status.value)
  }
  protected def httpRequestToCrawlerHttpRequest(httpRequest: HttpRequest):CrawlerHttpRequest = {
    CrawlerHttpRequest(httpMethodToCrawlerMethod(httpRequest.method), httpRequest.uri.toString())
  }

  protected def crawlerMethodToHttpMethod(cm: CrawlerHttpMethod): HttpMethod = {
    cm match {
      case CrawlerHttpMethod.GET => HttpMethods.GET
      case m => throw UnsupportedMethodException(m.toString)
    }
  }

  protected def httpMethodToCrawlerMethod(hm:HttpMethod):CrawlerHttpMethod = {
    hm match {
      case HttpMethods.GET => CrawlerHttpMethod.GET
      case m => throw UnsupportedMethodException(m.toString)
    }
  }

  protected def generateSteps(lastResponse:Option[HttpResponse]):List[CrawlerStep] = {

    val allResponses: List[HttpResponse] = lastResponse.map(r => responses :+ r).getOrElse(responses)

    var optionResponses:List[Option[CrawlerHttpResponse]] = allResponses.map(httpResponseToCrawlerHttpResponse).map(Some(_))

    if(allResponses.size<requests.size){
      optionResponses = optionResponses :+ None
    }

    requests.map(httpRequestToCrawlerHttpRequest).zip(optionResponses).map(ct => CrawlerStep(ct._1, ct._2))
  }
}

