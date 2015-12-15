import java.net.InetSocketAddress

import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import netty.codec.coap.{CoapMessageEncoder, CoapMessageDecoder}
import netty.reverse.ReversedClient

/**
  * Created by ytaras on 12/15/15.
  */
object Main extends App {

  val addr = new InetSocketAddress(1234)
  val server = new ReversedClient[SocketChannel](addr, classOf[NioServerSocketChannel]) {
    override def initChannel(ch: SocketChannel): Unit = {
      ch.pipeline()
        .addLast(new CoapMessageDecoder)
        .addLast(new CoapMessageEncoder)
    }
  }
  server.start()

}
