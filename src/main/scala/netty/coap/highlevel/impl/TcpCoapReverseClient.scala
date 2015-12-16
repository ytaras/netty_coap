package netty.coap.highlevel.impl

import java.net.InetSocketAddress
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.SocketChannel
import io.netty.util.internal.logging.InternalLoggerFactory
import netty.coap.highlevel.{CoapReverseClient, TcpServer}
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.core.network.config.NetworkConfig
import org.eclipse.californium.elements.{ConnectorBase, RawData}

/**
  * Created by ytaras on 12/16/15.
  */
abstract class TcpCoapReverseClient extends TcpServer with CoapReverseClient {
  val adapter = new TcpConnectorAdapter(bindTo)
  val endpoint = new CoapEndpoint(adapter, NetworkConfig.getStandard)
  override def onChannelActive(ctx: ChannelHandlerContext): Unit = {
    val remoteAddr = ctx.channel().asInstanceOf[SocketChannel].remoteAddress()
    val url = s"coap://$remoteAddr"
    val client = new CoapClient(url)
    client.setEndpoint(endpoint)
    newClientActive(new CoapClientWrapper(client))
  }

  override def socketInit(ch: SocketChannel): Unit = {
    ch.pipeline().addLast(new RawDataCodec())
  }
}

class TcpConnectorAdapter(inetAddress: InetSocketAddress) extends ConnectorBase(inetAddress) {
  val logger = InternalLoggerFactory.getInstance(getClass)
  val inboundQueue = new ConcurrentLinkedQueue[RawData]()
  val connectionRegistry = new ConcurrentHashMap[InetSocketAddress, SocketChannel]()
  override def getName: String = s"TCP connector on $inetAddress"

  override def sendNext(raw: RawData): Unit =
    Option(connectionRegistry.get(raw.getInetSocketAddress)) match {
      case Some(ch) => ch.writeAndFlush(raw)
      case None => logger.warn(s"Dropping message to ${raw.getInetSocketAddress} with size ${raw.getSize}")
    }

  override def receiveNext(): RawData = inboundQueue.poll()

  def inboundMessage(msg: RawData) = inboundQueue.add(msg)

  def startedConnection(socket: SocketChannel) =
    connectionRegistry.put(socket.remoteAddress(), socket)
  def stoppedConnection(socket: SocketChannel) =
    connectionRegistry.remove(socket.remoteAddress())

  def debug() = {
    import scala.collection.JavaConversions._
    connectionRegistry.keys().foreach { key =>
      logger.debug(s"Channel: $key --> ${connectionRegistry.get(key)}")
    }
    inboundQueue.foreach { data =>
      logger.debug(s"Inbound Queue member - ${data.getSize} bytes from ${data.getInetSocketAddress}")
    }
  }
}
