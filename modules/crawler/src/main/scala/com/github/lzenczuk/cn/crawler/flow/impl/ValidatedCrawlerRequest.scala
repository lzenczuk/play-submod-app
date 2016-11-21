package com.github.lzenczuk.cn.crawler.flow.impl

import akka.http.scaladsl.model.HttpRequest
import com.github.lzenczuk.cn.crawler.domain.CrawlerRequest

/**
  * Created by dev on 21/11/16.
  */
trait ValidatedCrawlerRequest
case class InvalidCrawlerRequest(error: String, crawlerRequest: CrawlerRequest) extends ValidatedCrawlerRequest
case class ValidCrawlerRequest(httpRequest: HttpRequest, crawlerRequest: CrawlerRequest) extends ValidatedCrawlerRequest
