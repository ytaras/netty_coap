package netty.coap

import io.netty.channel.socket.SocketChannel
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.core.network.config.NetworkConfig

/**
  * Created by ytaras on 12/15/15.
  */
class TcpClient {
  def establised(ch: SocketChannel): CoapClient = {
    val endpoint = new CoapEndpoint(new TcpSocketConnector(ch), NetworkConfig.getStandard)
    val client = new CoapClient()
    client.setEndpoint(endpoint)
    client
  }

}
