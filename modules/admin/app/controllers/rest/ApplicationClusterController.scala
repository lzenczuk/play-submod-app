package controllers.rest

import javax.inject.{Inject, Named, Singleton}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.github.lzenczuk.cn.cluster.actor.ApplicationClusterActor
import com.github.lzenczuk.cn.cluster.domain.ClusterChange
import controllers.ClusterModelJsonMapper
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.duration._

/**
  * Created by dev on 14/11/16.
  */

case class ClusterAddress(protocol: String, system: String, host: String, port: Int)

@Singleton
class ApplicationClusterController @Inject()(@Named("application-cluster-actor") applicationClusterActor: ActorRef) extends Controller{
  import ClusterModelJsonMapper.clusterChangeWrites

  implicit val actorAskTimeout: Timeout = 5.seconds

  implicit val clusterAddressReads = Json.reads[ClusterAddress]

  def status = Action.async{
    (applicationClusterActor ? ApplicationClusterActor.GetClusterState).mapTo[ClusterChange].map{
      ns => Ok(Json.toJson(ns))
    }
  }

  def createCluster = Action{
    applicationClusterActor ! ApplicationClusterActor.CreateCluster
    Ok(Json.obj("status" -> "OK", "message" -> "Create cluster request send"))
  }

  def joinCluster = Action(BodyParsers.parse.json){ request =>

    val clusterAddressResult = request.body.validate[ClusterAddress]

    clusterAddressResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"OK", "message" -> JsError.toJson(errors)))
      },
      clusterAddress => {
        applicationClusterActor ! ApplicationClusterActor.JoinCluster(clusterAddress.protocol, clusterAddress.system, Some(clusterAddress.host), Some(clusterAddress.port))
        Ok(Json.obj("status" -> "OK", "message" -> "Join cluster request send"))
      }
    )
  }

  def leaveCluster = Action{
    applicationClusterActor ! ApplicationClusterActor.LeaveCluster
    Ok(Json.obj("status" -> "OK", "message" -> "Leave cluster request send"))
  }

}
