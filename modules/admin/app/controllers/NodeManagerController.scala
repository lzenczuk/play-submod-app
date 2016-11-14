package controllers

import javax.inject.{Inject, Named, Singleton}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._

/**
  * Created by dev on 14/11/16.
  */
@Singleton
class NodeManagerController @Inject()(@Named("node-manager-actor") nodeManagerActor: ActorRef) extends Controller{

  implicit val timeout: Timeout = 5.seconds

  def ping = Action.async{
    (nodeManagerActor ? "Hello").mapTo[String].map{
      msg => Ok(msg)
    }
  }

}
