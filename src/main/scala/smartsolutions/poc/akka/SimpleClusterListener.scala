package smartsolutions.poc.akka

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

/**
  * Created by ytaras on 12/7/15.
  */
class SimpleClusterListener extends Actor with ActorLogging {
  val cluster = Cluster(context.system)

  override def preStart = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }
  override def postStop = cluster unsubscribe self

  override def receive: Receive = {
    case x: ClusterDomainEvent => log info s"Message: ${x}"
  }
}
