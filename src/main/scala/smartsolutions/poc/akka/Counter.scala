package smartsolutions.poc.akka

import akka.actor.{ActorRef, ActorLogging, Actor}
import akka.actor.Actor.Receive
import smartsolutions.poc.akka.Counter.{Get, Inc, Dec}

/**
  * Created by ytaras on 12/7/15.
  */
class Counter extends Actor with ActorLogging {
  var count = 0

  override def receive: Receive = {
    case Inc => {
      count += 1
      log.info("Increment")
    }
    case Dec => count -= 1
    case Get(x) => {
      log.info("Get!")
      x ! count
    }
  }
}
object Counter {
  case object Inc
  case object Dec
  case class Get(replyTo: ActorRef)
}
