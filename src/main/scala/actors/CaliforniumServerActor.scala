package actors

import akka.actor.{Actor, ActorLogging}
import coap.CaliforniumServer
import coap.CaliforniumServer.{Put, Get}

/**
  * Created by ytaras on 12/8/15.
  */
class CaliforniumServerActor extends Actor with ActorLogging {
  val server = CaliforniumServer(self)(context.dispatcher)
  var value = ""
  override def receive: Receive = {
    case Get(_) => sender ! value
    case Put(x, _) => value = x
  }
}
