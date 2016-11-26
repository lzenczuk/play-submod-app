package com.github.lzenczuk.cn.crawler.domain

/**
  * Created by dev on 21/11/16.
  */

trait CrawlerHttpMethod
object CrawlerHttpMethod {
  case object GET extends CrawlerHttpMethod
}

case class CrawlerHttpRequest(method: CrawlerHttpMethod, url:String)
case class CrawlerHttpResponse(responseCode:Int, responseString:String)

case class CrawlerStep(crawlerHttpRequest: CrawlerHttpRequest, crawlerHttpResponseOption: Option[CrawlerHttpResponse])

case class CrawlerRequest(method: CrawlerHttpMethod, url:String, redirect:Boolean)

case class CrawlerResponse(
                            error:Option[String],
                            request: CrawlerRequest,
                            steps: List[CrawlerStep] = List()){
  def successful = error.isEmpty
  def urls = steps.map(_.crawlerHttpRequest.url)
}
