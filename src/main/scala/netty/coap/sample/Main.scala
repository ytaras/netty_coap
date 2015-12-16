package netty.coap.sample

import java.net.InetSocketAddress

import netty.coap.Client
import netty.coap.impl.TcpCoapReverseClient
import org.eclipse.californium.core.coap.CoAP.Code
import org.eclipse.californium.core.coap.{Response, Request}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ytaras on 12/16/15.
  */
object Main extends App {
  val addr = new InetSocketAddress(1234)
  val server = new SampleReverseClient(addr)
  server.start()

}
class SampleReverseClient(addr: InetSocketAddress) extends TcpCoapReverseClient(addr) {

  override def newClientActive(client: Client): Unit = {
    val req = new Request(Code.GET)
    req.setURI("/version")
    client.request(req).onSuccess {
      case resp =>
        println(s"Received ${resp.getResponseText}")
        client.close()
    }
    debug()
  }
}
