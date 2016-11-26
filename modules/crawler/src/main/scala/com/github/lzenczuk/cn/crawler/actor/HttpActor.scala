package com.github.lzenczuk.cn.crawler.actor

import akka.NotUsed
import akka.actor.{ActorRef, FSM, Status}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.github.lzenczuk.cn.crawler.actor.HttpActor.{BusyWaitingForResponse, HttpActorException}
import com.github.lzenczuk.cn.crawler.actor.HttpActor.fsm.{Data, State}

import scala.util.{Failure, Success, Try}

/**
  * Created by dev on 24/11/16.
  */

object HttpActor {

  case class BusyWaitingForResponse(request: HttpRequest)

  object fsm {
    sealed trait State
    case object WaitingForRequest extends State
    case object WaitingForResponse extends State

    sealed trait Data
    case object Empty extends Data
    case class Recipient(actorRef: ActorRef) extends Data
  }

  case class HttpActorException(message:String) extends RuntimeException(message)
}

class HttpActor extends FSM[State, Data] {
  import HttpActor.fsm._

  case object HttpClientFlowComplete

  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  var httpClientFlowActor = createHttpClientFlow()

  override def postStop(): Unit = {
    stateData match {
      case Recipient(recipient) =>
        recipient ! Failure(HttpActorException(s"Http actor stopped."))
    }
  }

  startWith(WaitingForRequest, Empty)

  when(WaitingForRequest){
    case Event(request:HttpRequest, Empty) =>
      httpClientFlowActor ! (request, NotUsed)
      goto(WaitingForResponse) using Recipient(sender)
  }

  when(WaitingForResponse){
    case Event((response @ Success(_:HttpResponse), NotUsed), Recipient(recipient)) =>
      recipient ! response
      goto(WaitingForRequest) using Empty
    case Event((response @ Failure(ex), NotUsed), Recipient(recipient)) =>
      recipient ! response
      goto(WaitingForRequest) using Empty
    case Event(Status.Failure(ex), Recipient(recipient)) =>
      recipient ! Failure(ex)
      resetHttpClientFlow()
      goto(WaitingForRequest) using Empty
    case Event(request:HttpRequest, r:Recipient) =>
      sender ! BusyWaitingForResponse(request)
      stay()
  }

  initialize()

  private def createHttpClientFlow():ActorRef = {

    type HttpClientFlowRequest = (HttpRequest, NotUsed)
    type HttpClientFlowResponse = (Try[HttpResponse], NotUsed)

    val clientFlow: Flow[HttpClientFlowRequest, HttpClientFlowResponse, NotUsed] = Http(context.system).superPool[NotUsed]()

    Source.actorRef[HttpClientFlowRequest](1, OverflowStrategy.fail)
      .via(clientFlow)
      .to(Sink.actorRef[HttpClientFlowResponse](self, HttpClientFlowComplete))
      .run()
  }

  private def resetHttpClientFlow(): Unit ={
    httpClientFlowActor = createHttpClientFlow()
  }

}
