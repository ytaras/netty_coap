package smartsolutions.poc

import _root_.akka.actor._
import _root_.akka.cluster.Cluster
import _root_.akka.pattern.ask
import _root_.akka.routing.BalancingPool
import com.typesafe.config.ConfigFactory
import smartsolutions.poc.akka.Counter.{Get, Inc}
import smartsolutions.poc.akka.{FactorialFrontend, FactorialBackend, Counter, SampleActor}

import scala.concurrent.duration._
import scala.util.{Success, Failure}

/**
  * Created by ytaras on 12/7/15.
  */
object Main extends App {

  Seq("2551", "2552", "0") foreach { port =>
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${port}")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]"))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", config)
    system.actorOf(Props[FactorialBackend], name = "factorialBackend")
  }

  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=0")
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]"))
    .withFallback(ConfigFactory.load())

  val frontendSystem = ActorSystem("ClusterSystem", config)
  Cluster(frontendSystem) registerOnMemberUp {
    frontendSystem.actorOf(Props(classOf[FactorialFrontend], 200, true), name = "factorialFrontend") ! "start"
  }
  Cluster(frontendSystem) registerOnMemberRemoved {
    frontendSystem.registerOnTermination(System.exit(-1))
    frontendSystem.scheduler.scheduleOnce(10.seconds)(System.exit(-1))(frontendSystem.dispatcher)
    frontendSystem.terminate()
  }
  class Listener extends Actor with ActorLogging {
    def receive = {
      case d : DeadLetter => log.info(s"Dead letter: ${d.recipient.path}")
    }
  }
//  val listener = frontendSystem.actorOf(Props[Listener])
//  frontendSystem.eventStream.subscribe(listener, classOf[DeadLetter])

}
