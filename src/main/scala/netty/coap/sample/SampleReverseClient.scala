package netty.coap.sample

import java.net.InetSocketAddress

import io.netty.util.internal.logging.InternalLoggerFactory
import netty.coap.Client
import netty.coap.impl.TcpCoapReverseClient
import org.eclipse.californium.core.{CoapResponse, CoapHandler, CoapClient}
import org.eclipse.californium.core.coap.CoAP.Code
import org.eclipse.californium.core.coap.{Response, Request}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ytaras on 12/16/15.
  */
object SampleReverseClient extends App {
  val addr = new InetSocketAddress(1234)
  val server = new SampleReverseClient(addr)
  server.start()

}
class SampleReverseClient(addr: InetSocketAddress) extends TcpCoapReverseClient(addr) {
  val observeHandler = new CoapHandler {
    override def onError(): Unit = logger.error("Unknown error")

    override def onLoad(response: CoapResponse): Unit = {
      logger.info(s"Received response to OBSERVE -> ${response.getResponseText}")
    }
  }

  override def newClientActive(client: Client[CoapClient]): Unit = {
    val req = new Request(Code.GET)
    req.setURI("/version")
    client.request(req).onSuccess {
      case resp =>
        logger.info(s"Received response to GET --> ${resp.getResponseText}")
        client.underlyingClient.observe(observeHandler)
    }
    debug()
  }
}
