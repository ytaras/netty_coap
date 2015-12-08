import actors.CaliforniumServerActor
import akka.actor.{ActorSystem, Props}

/**
  * Created by ytaras on 12/8/15.
  */
object Main extends App {

  val system = ActorSystem("CoapSystem")
  val server = system.actorOf(Props[CaliforniumServerActor])
}
