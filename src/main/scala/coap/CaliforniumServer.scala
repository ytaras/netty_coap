package coap

import akka.actor.ActorRef
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
class CaliforniumServer(actor: ActorRef)(implicit ec: ExecutionContext) extends CoapServer {
  add(new HelloWorldResource(actor))
}

object CaliforniumServer {

  def apply(actor: ActorRef)(implicit ec: ExecutionContext) = {
    val server = new CaliforniumServer(actor)
    server.start()
    server
  }

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

class HelloWorldResource(actor: ActorRef)(implicit val ec: ExecutionContext) extends CoapResource("helloWorld") {
  setObservable(true)
  getAttributes.setTitle("Hello world resource")

  override def handlePUT(exchange: CoapExchange): Unit = {
    actor ! Exchange(exchange, getPath)
    exchange.respond(CoAP.ResponseCode.CHANGED)
    changed()
  }

  override def handleGET(exchange: CoapExchange): Unit = {
    exchangeFuture(exchange) foreach { result =>
      exchange.respond(CoAP.ResponseCode.CONTENT, result.toString)
    }
  }
  def exchangeFuture(exchange: CoapExchange): Future[Any] = ask(actor, Exchange(exchange, getPath))(10.seconds)
}
