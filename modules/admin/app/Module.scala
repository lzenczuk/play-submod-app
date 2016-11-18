import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.github.lzenczuk.cn.cluster.actor.ApplicationClusterActor
import com.github.lzenczuk.cn.cluster.domain.ApplicationCluster
import com.github.lzenczuk.cn.cluster.domain.impl.ApplicationClusterImpl
import com.google.inject.{AbstractModule, Provides}
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  *
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

    bind(classOf[ApplicationCluster]).to(classOf[ApplicationClusterImpl])
    bindActor[ApplicationClusterActor]("application-cluster-actor")
  }

}

