package controllers

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import com.github.lzenczuk.cn.cluster.actor.ClusterNotificationActor.SubscribeClusterStatus
import com.github.lzenczuk.cn.cluster.domain.{ClusterChange, NodeChange, NodeState}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Controller, WebSocket}

/**
  * Created by dev on 18/11/16.
  */

object NotificationActor {
  def props(out:ActorRef, clusterNotificationActor: ActorRef) = Props(new NotificationActor(out, clusterNotificationActor))
}

class NotificationActor(out:ActorRef, clusterNotificationActor: ActorRef) extends Actor with ActorLogging {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    clusterNotificationActor ! SubscribeClusterStatus
  }

  implicit val nodeStateWrites = new Writes[NodeState] {
    override def writes(ns: NodeState): JsValue = {

      val state = ns match {
        case NodeState.Down => "Down"
        case NodeState.Exiting => "Exiting"
        case NodeState.Joining => "Joining"
        case NodeState.Leaving => "Leaving"
        case NodeState.Removed => "Removed"
        case NodeState.Up => "Up"
        case NodeState.WeaklyUp => "WeaklyUp"
        case NodeState.Unknown => "Unknown"
        case _ => "__JSON_ERROR__UNMATCHED_STATE__"
      }

      Json.obj(
        "nodeStatus" -> state
      )
    }
  }

  implicit val nodeChangeWrites = Json.writes[NodeChange]
  implicit val clusterChangeWrites = Json.writes[ClusterChange]

  def receive = {
    case cs:ClusterChange => out ! Json.toJson(cs)
  }
}

@Singleton
class NotificationController @Inject() (implicit system: ActorSystem, materializer: Materializer, @Named("cluster-notification-actor") clusterNotificationActor: ActorRef) extends Controller{

  def socket = WebSocket.accept[JsValue,JsValue]{ request =>
    ActorFlow.actorRef(out => NotificationActor.props(out, clusterNotificationActor))
  }

}
