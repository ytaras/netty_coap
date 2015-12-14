import java.net.InetSocketAddress

import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.core.network.config.NetworkConfig
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapResource, CoapServer}

/**
  * Created by ytaras on 12/14/15.
  */
object CaliforniumTcpServer extends App {
  val port = 1234
  val server = new Server
  server.addEndpoint(new CoapEndpoint(new InetSocketAddress("127.0.0.1", 1234)))
//  server.addEndpoint(new CoapEndpoint(new TcpConnector, NetworkConfig.getStandard))
  server.start()
}

class Server extends CoapServer {
  add(new HelloEndpoint)
}

class HelloEndpoint extends CoapResource("helloWorld") {
  getAttributes.setTitle("Hello world resource")

  override def handleGET(exchange: CoapExchange): Unit = {
    exchange.respond("Hello, World")
  }
}

