package controllers

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}
import akka.actor.ActorSystem
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import com.github.lzenczuk.cn.cluster.NodeManagerActor

/**
  * Created by dev on 14/11/16.
  */
@Singleton
class NodeManagerController @Inject()(system: ActorSystem) extends Controller{

  implicit val timeout: Timeout = 5.seconds

  val nodeManagerActor = system.actorOf(NodeManagerActor.props)

  def ping = Action.async{
    (nodeManagerActor ? "Hello").mapTo[String].map{
      msg => Ok(msg)
    }
  }

}
