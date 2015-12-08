import actors.CaliforniumServerActor
import akka.actor.{ActorSystem, Props}
import org.apache.spark.SparkEnv

/**
  * Created by ytaras on 12/8/15.
  */
object Main extends App {

//  val system = ActorSystem("CoapSystem")
  SparkEtl
  val system = SparkEnv.get.actorSystem
  val server = system.actorOf(Props[CaliforniumServerActor])
  SparkEtl.start(server.path)
}
