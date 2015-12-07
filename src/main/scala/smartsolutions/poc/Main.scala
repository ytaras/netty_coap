package smartsolutions.poc

import _root_.akka.actor.{Props, ActorSystem}
import smartsolutions.poc.akka.SampleActor

/**
  * Created by ytaras on 12/7/15.
  */
object Main extends App {
  val system = ActorSystem("helloakka")
  val greeter = system.actorOf(Props[SampleActor], "logger")
  greeter ! "Hello, world!"

}
