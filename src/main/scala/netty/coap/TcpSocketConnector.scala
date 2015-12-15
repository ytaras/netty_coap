package netty.coap

import java.net.InetSocketAddress

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.channel.socket.SocketChannel
import io.netty.util.internal.logging.InternalLoggerFactory
import org.eclipse.californium.elements.{RawData, RawDataChannel, Connector}

/**
  * Created by ytaras on 12/15/15.
  */
class TcpSocketConnector(ch: SocketChannel) extends Connector {
  var messageHandler: RawDataChannel = null
  val handler = new SimpleChannelInboundHandler[RawData]() {
    override def channelRead0(ctx: ChannelHandlerContext, msg: RawData): Unit = {
      messageHandler.receiveData(msg)
    }
  }

  val logger = InternalLoggerFactory.getInstance(classOf[TcpSocketConnector])
  override def stop(): Unit = {
    ch.pipeline().remove(handler)
    logger.info("Stop")
  }

  override def destroy(): Unit = {
    ch.close()
  }

  override def send(msg: RawData): Unit = ch.writeAndFlush(msg)

  override def setRawDataReceiver(messageHandler: RawDataChannel): Unit = {
    this.messageHandler = messageHandler
  }

  override def getAddress: InetSocketAddress = ch.localAddress()

  override def start(): Unit = {
    logger.info("Start")
    ch.pipeline().addLast("tcp coap", handler)
  }
}
