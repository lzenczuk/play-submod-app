import javax.inject.{Inject, Provider, Singleton}

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.github.lzenczuk.cn.cluster.actor.{ClusterNotificationActor, NodeManagerActor}
import com.github.lzenczuk.cn.cluster.domain.ApplicationCluster
import com.github.lzenczuk.cn.cluster.domain.impl.ApplicationClusterImpl
import com.google.inject.{AbstractModule, Provides}
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.

  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module extends AbstractModule with AkkaGuiceSupport{

  @Provides
  def provideCluster(actorSystem: ActorSystem):Cluster = {
    Cluster(actorSystem)
  }

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    //bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)

    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    //bind(classOf[ApplicationTimer]).asEagerSingleton()

    // Set AtomicCounter as the implementation for Counter.
    //bind(classOf[Counter]).to(classOf[AtomicCounter])

    bind(classOf[ApplicationCluster]).to(classOf[ApplicationClusterImpl])
    bindActor[NodeManagerActor]("node-manager-actor")
    bindActor[ClusterNotificationActor]("cluster-notification-actor")
  }

}
/*
@Singleton
class ClusterProvider @Inject() (actorSystem: ActorSystem) extends Provider[Cluster] {

  println("--------------> Cluster provider created")

  lazy val get: Cluster = {
    println("--------------> Creating new cluster instance")
    Cluster(actorSystem)
  }
}*/

