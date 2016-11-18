package controllers

import com.github.lzenczuk.cn.cluster.domain.{ClusterChange, NodeChange, NodeState}
import play.api.libs.json.{JsValue, Json, Writes}

/**
  * Created by dev on 18/11/16.
  */
object ClusterModelJsonMapper {

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

}
