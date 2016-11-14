package controllers

import javax.inject.{Inject, Named, Singleton}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.github.lzenczuk.cn.cluster.NodeManagerActor
import com.github.lzenczuk.cn.cluster.NodeManagerActor._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsResult, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.duration._

/**
  * Created by dev on 14/11/16.
  */
@Singleton
class NodeManagerController @Inject()(@Named("node-manager-actor") nodeManagerActor: ActorRef) extends Controller{

  implicit val actorAskTimeout: Timeout = 5.seconds

  implicit val clusterMemberWrites = Json.writes[ClusterMember]
  implicit val nodeStatusWrites = Json.writes[NodeStatus]

  implicit val clusterAddressReads = Json.reads[ClusterAddress]

  def status = Action.async{
    (nodeManagerActor ? NodeManagerActor.GetNodeStatus).mapTo[NodeStatus].map{
      ns => Ok(Json.toJson(ns))
    }
  }

  def createCluster = Action{
    nodeManagerActor ! CreateCluster
    Ok(Json.obj("status" -> "OK", "message" -> "Create cluster request send"))
  }

  def joinCluster = Action(BodyParsers.parse.json){ request =>

    val clusterAddressResult = request.body.validate[ClusterAddress]

    clusterAddressResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"OK", "message" -> JsError.toJson(errors)))
      },
      clusterAddress => {
        nodeManagerActor ! JoinCluster(clusterAddress)
        Ok(Json.obj("status" -> "OK", "message" -> "Join cluster request send"))
      }
    )
  }

  def leaveCluster = Action{
    nodeManagerActor ! LeaveCluster
    Ok(Json.obj("status" -> "OK", "message" -> "Leave cluster request send"))
  }

}
