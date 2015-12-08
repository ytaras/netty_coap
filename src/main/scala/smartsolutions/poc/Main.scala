package smartsolutions.poc

import _root_.akka.actor.{ActorSystem, Props}
import _root_.akka.pattern.ask
import _root_.akka.routing.BalancingPool
import com.typesafe.config.ConfigFactory
import smartsolutions.poc.akka.Counter.{Get, Inc}
import smartsolutions.poc.akka.{Counter, SampleActor}

import scala.concurrent.duration._
import scala.util.{Success, Failure}

/**
  * Created by ytaras on 12/7/15.
  */
object Main extends App {

  val systems = Seq("2551", "2552", "0") map { port =>
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${port}")
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", config)
    system
  }

  val system = systems.head
  val logger = system.actorOf(Props[SampleActor])
  val router = system.actorOf(BalancingPool(5).props(Props[Counter]), "router")
  for {
    i <- 1 to 10000
  } router ! Inc
  implicit val timeout = _root_.akka.util.Timeout(5.seconds)
  import scala.concurrent.ExecutionContext.Implicits._
  logger ! 1
  router ! Get(logger)
}
