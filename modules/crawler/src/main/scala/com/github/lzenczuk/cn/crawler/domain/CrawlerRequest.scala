package com.github.lzenczuk.cn.crawler.domain

/**
  * Created by dev on 21/11/16.
  */

trait CrawlerHttpMethod
object CrawlerHttpMethod {
  case object GET extends CrawlerHttpMethod
}

case class CrawlerHttpResponse(responseCode:Int, responseString:String)

case class CrawlerRequest(method: CrawlerHttpMethod, url:String, redirect:Boolean)

case class CrawlerResponse(error:Option[String], request: CrawlerRequest, responses:List[CrawlerHttpResponse]){
  def successful = error.isEmpty
  def resultResponse = responses.tail
}
