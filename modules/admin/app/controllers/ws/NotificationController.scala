package controllers.ws

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import com.github.lzenczuk.cn.cluster.actor.ApplicationClusterActor.SubscribeClusterStatus
import com.github.lzenczuk.cn.cluster.domain.ClusterChange
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc.{Controller, WebSocket}

/**
  * Created by dev on 18/11/16.
  */

object NotificationActor {
  def props(out:ActorRef, clusterNotificationActor: ActorRef) = Props(new NotificationActor(out, clusterNotificationActor))
}

class NotificationActor(out:ActorRef, clusterNotificationActor: ActorRef) extends Actor with ActorLogging {
  import controllers.ClusterModelJsonMapper.clusterChangeWrites

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    clusterNotificationActor ! SubscribeClusterStatus
  }

  def receive = {
    case cs:ClusterChange => out ! Json.toJson(cs)
  }
}

@Singleton
class NotificationController @Inject() (implicit system: ActorSystem, materializer: Materializer, @Named("application-cluster-actor") applicationClusterActor: ActorRef) extends Controller{

  def socket = WebSocket.accept[JsValue,JsValue]{ request =>
    ActorFlow.actorRef(out => NotificationActor.props(out, applicationClusterActor))
  }

}
