package netty.coap.highlevel.impl

import java.net.InetSocketAddress
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.channel.socket.SocketChannel
import io.netty.util.internal.logging.InternalLoggerFactory
import netty.coap.highlevel.{Client, CoapReverseClient, TcpServer}
import org.eclipse.californium.core.CoapClient
import org.eclipse.californium.core.coap.{Message, Request, EmptyMessage, Response}
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.core.network.config.NetworkConfig
import org.eclipse.californium.core.network.interceptors.{MessageTracer, MessageInterceptor}
import org.eclipse.californium.elements.{ConnectorBase, RawData}

/**
  * Created by ytaras on 12/16/15.
  */
abstract class TcpCoapReverseClient(val bindTo: InetSocketAddress) extends TcpServer with CoapReverseClient {
  val adapter = new TcpConnectorAdapter(bindTo)
  val endpoint = new CoapEndpoint(adapter, NetworkConfig.getStandard)
  endpoint.addInterceptor(new TcpConnectorAwareCancelInterceptor(adapter))
  val adapterHandler = new TcpConnectorAdapterHandler(adapter)

  override def onChannelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info("two")
    newClientActive(buildCoapClient(remoteAddr(ctx)))
  }

  def socket(ctx: ChannelHandlerContext): SocketChannel =
    ctx.channel().asInstanceOf[SocketChannel]

  def remoteAddr(ctx: ChannelHandlerContext) =
    socket(ctx).remoteAddress()

  def buildCoapClient(remoteAddr: InetSocketAddress) = {
    val url = s"coap://${remoteAddr.getHostString}:${remoteAddr.getPort}/"
    val client = new CoapClient(url)
    client.setEndpoint(endpoint)
    new CoapClientWrapper(client)
  }

  override def socketInit(ch: SocketChannel): Unit = {
    ch.pipeline().addLast(new RawDataCodec()).addLast(adapterHandler)
  }

  def debug() = adapter.debug()
}

class TcpConnectorAwareCancelInterceptor(adapter: TcpConnectorAdapter) extends MessageInterceptor {
  val logger = InternalLoggerFactory.getInstance(getClass)
  def address(message: Message): InetSocketAddress =
    new InetSocketAddress(message.getDestination, message.getDestinationPort)

  def cancelIfNotPresent(message: Message) =
    if(!adapter.isOpen(address(message))) {
      logger.info(s"Canceling message $message because it's destination ${address(message)} is not active")
      message.cancel()
    }

  override def sendEmptyMessage(message: EmptyMessage): Unit = cancelIfNotPresent(message)

  override def receiveRequest(request: Request): Unit = {}

  override def sendRequest(request: Request): Unit = cancelIfNotPresent(request)

  override def receiveResponse(response: Response): Unit = {}

  override def receiveEmptyMessage(message: EmptyMessage): Unit = {}

  override def sendResponse(response: Response): Unit = cancelIfNotPresent(response)
}

@Sharable
class TcpConnectorAdapterHandler(connector: TcpConnectorAdapter) extends ChannelInboundHandlerAdapter {
  val logger = InternalLoggerFactory.getInstance(getClass)

  def socket(ctx: ChannelHandlerContext) = ctx.channel().asInstanceOf[SocketChannel]

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    logger.info("One")
    connector.startedConnection(socket(ctx))
    super.channelActive(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    connector.inboundMessage(msg.asInstanceOf[RawData])
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    connector.stoppedConnection(socket(ctx))
    super.channelInactive(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error("Exception caught", cause)
    connector.stoppedConnection(socket(ctx))
    ctx.close()
  }
}

class TcpConnectorAdapter(inetAddress: InetSocketAddress) extends ConnectorBase(inetAddress) {
  def isOpen(address: InetSocketAddress): Boolean = connectionRegistry.containsKey(address)

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
