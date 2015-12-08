package coap

import actors.{ResultsActor, PublishValueActor}
import akka.actor.{Props, ActorSystem, ActorRef}
import akka.pattern.ask
import coap.CaliforniumServer.Exchange
import org.eclipse.californium.core.coap.CoAP
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapResource, CoapServer}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by ytaras on 12/8/15.
  */
class CaliforniumServer(system: ActorSystem) extends CoapServer {
  implicit val ec = system.dispatcher
  val dataStreamActor = system.actorOf(Props[PublishValueActor], "CoapPublisher")
  add(new PublishValueResource(dataStreamActor))
  val resultsActor = system.actorOf(Props[ResultsActor], "ResultsFromSpark")
  add(new ResultsCoapResource(resultsActor))

  start()
}

object CaliforniumServer {

  sealed trait Exchange {
    def path: String
  }
  object Exchange {
    def apply(coapExchange: CoapExchange, path: String) = {
      coapExchange.getRequestCode match {
        case CoAP.Code.GET => Get(path)
        case CoAP.Code.PUT => Put(coapExchange.getRequestText, path)
        case CoAP.Code.DELETE => Delete(path)
        case CoAP.Code.POST => Post(path)
      }
    }
  }
  case class Get(path: String) extends Exchange
  case class Delete(path: String) extends Exchange
  case class Post(path: String) extends Exchange
  case class Put(text: String, path: String) extends Exchange
}

class PublishValueResource(actor: ActorRef)(implicit val ec: ExecutionContext) extends CoapResource("helloWorld") {
  setObservable(true)
  getAttributes.setTitle("publish data")

  override def handlePUT(exchange: CoapExchange): Unit = {
    actor ! Exchange(exchange, getPath)
    exchange.respond(CoAP.ResponseCode.CHANGED)
  }

}
class ResultsCoapResource(actor: ActorRef)(implicit val ec: ExecutionContext) extends CoapResource("results") {
  actor ! this
  setObservable(true)
  getAttributes.setTitle("observe results")

  override def handleGET(exchange: CoapExchange): Unit = {
    exchangeFuture(exchange) foreach { result =>
      exchange.respond(CoAP.ResponseCode.CONTENT, result.toString)
    }
  }
  def exchangeFuture(exchange: CoapExchange): Future[Any] = ask(actor, Exchange(exchange, getPath))(10.seconds)
}
