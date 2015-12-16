package netty.coap.highlevel.impl

import java.net.InetSocketAddress
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
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
abstract class TcpCoapReverseClient(val bindTo: InetSocketAddress) extends TcpServer with CoapReverseClient {
  val adapter = new TcpConnectorAdapter(bindTo)
  val endpoint = new CoapEndpoint(adapter, NetworkConfig.getStandard)
  val adapterHandler = new TcpConnectorAdapterHandler(adapter)
  override def onChannelActive(ctx: ChannelHandlerContext): Unit = {
    val socket = ctx.channel().asInstanceOf[SocketChannel]
    val remoteAddr = socket.remoteAddress()
    val url = s"coap://${remoteAddr.getHostString}:${remoteAddr.getPort}/"
    logger.error(url)
    val client = new CoapClient(url)
    client.setEndpoint(endpoint)
    newClientActive(new CoapClientWrapper(client))
    // TODO Handle connection closing
  }

  override def socketInit(ch: SocketChannel): Unit = {
    ch.pipeline().addLast(new RawDataCodec()).addLast(adapterHandler)
  }

  def debug() = adapter.debug()
}

class TcpConnectorAdapterHandler(connector: TcpConnectorAdapter) extends ChannelInboundHandlerAdapter {
  val logger = InternalLoggerFactory.getInstance(getClass)

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.debug(s"active $ctx")
    connector.startedConnection(ctx.channel().asInstanceOf[SocketChannel])
    super.channelActive(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    logger.debug(s"read from $ctx")
    connector.inboundMessage(msg.asInstanceOf[RawData])
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    logger.debug(s"close $ctx")
    connector.stoppedConnection(ctx.channel().asInstanceOf[SocketChannel])
    super.channelInactive(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error("Exception caught", cause)
    connector.stoppedConnection(ctx.channel().asInstanceOf[SocketChannel])
    ctx.close()
  }
}

class TcpConnectorAdapter(inetAddress: InetSocketAddress) extends ConnectorBase(inetAddress) {
  private val logger = InternalLoggerFactory.getInstance(getClass)
  private val inboundQueue = new ConcurrentLinkedQueue[RawData]()
  private val connectionRegistry = new ConcurrentHashMap[InetSocketAddress, SocketChannel]()
  override def getName: String = s"TCP connector on $inetAddress"

  override def sendNext(raw: RawData): Unit =
    Option(connectionRegistry.get(raw.getInetSocketAddress)) match {
      case Some(ch) => ch.writeAndFlush(raw)
      case None =>
        logger.warn(s"Dropping message to ${raw.getInetSocketAddress} with size ${raw.getSize}")
        debug()
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
