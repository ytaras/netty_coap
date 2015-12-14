import java.net.{DatagramPacket, DatagramSocket}
import java.util.concurrent.ScheduledThreadPoolExecutor

import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.coap.{Request, EmptyMessage, Response}
import org.eclipse.californium.core.network.interceptors.MessageInterceptor
import org.eclipse.californium.core.network.{Exchange, Endpoint, EndpointObserver, CoapEndpoint}
import org.eclipse.californium.core.server.MessageDeliverer

/**
  * Created by ytaras on 12/10/15.
  */
object ExampleUdpServer extends App {
  val endpoint = new CoapEndpoint(1234)
  endpoint.setMessageDeliverer( new MessageDeliverer {
    override def deliverRequest(exchange: Exchange): Unit = ???

    override def deliverResponse(exchange: Exchange, response: Response): Unit = ???
  })
  endpoint.setExecutor(new ScheduledThreadPoolExecutor(1))
  endpoint.start()
  print(new CoapClient("coap://127.0.0.1:1234/").get())
}
