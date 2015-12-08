package actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import coap.CaliforniumServer.{Get, Put}
import coap.ResultsCoapResource

class PublishValueActor extends Actor with ActorLogging {
  var subscribers: Set[ActorRef] = Set.empty
  def receive = {
    case Put(x, _) => subscribers foreach { _ ! x }
    case Subscribe => subscribers += sender()
  }
}

class ResultsActor extends Actor with ActorLogging {
  var value: String = ""
  var coapResource: ResultsCoapResource = null
  def receive = {
    case c: ResultsCoapResource => coapResource = c
    case Get(_) => sender ! value
    case x : String =>
      value = x
      coapResource.changed()
  }
}

case object Subscribe
