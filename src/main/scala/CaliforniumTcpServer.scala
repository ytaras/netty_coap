import java.net.InetSocketAddress
import java.util
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, TimeUnit}

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.internal.logging.InternalLoggerFactory
import netty.coap.highlevel.impl.RawDataCodec
import org.eclipse.californium.core._
import org.eclipse.californium.core.coap.Request
import org.eclipse.californium.core.network.CoapEndpoint
import org.eclipse.californium.core.network.config.NetworkConfig
import org.eclipse.californium.core.network.serialization.DataParser
import org.eclipse.californium.elements.{ConnectorBase, RawData}

/**
  * Created by ytaras on 12/14/15.
  */
object CaliforniumTcpServer extends App {
  val port = 1234
  val local = new InetSocketAddress("127.0.0.1", port)
//  val server = new Server
//  server.addEndpoint(new CoapEndpoint(new InetSocketAddress("127.0.0.1", 1234)))
////  server.addEndpoint(new CoapEndpoint(new TcpConnector, NetworkConfig.getStandard))
//  server.start()
  new CoapTcpServer(local).start
}
object WriteToConsole extends CoapHandler {
  val logger = InternalLoggerFactory.getInstance(getClass)

  override def onError(): Unit = logger.error("Some error!")

  override def onLoad(response: CoapResponse): Unit = logger.info(s"Loaded response $response")
}

class TcpConnector(inetSocketAddress: InetSocketAddress) extends ConnectorBase(inetSocketAddress) {
  def stopped(ctx: ChannelHandlerContext) = {
    channelsRegistry.remove(ctx.channel().remoteAddress())
  }

  def message(data: RawData): Unit = inboundMessageQueue.add(data)
  val inboundMessageQueue = new ConcurrentLinkedQueue[RawData]()
  val channelsRegistry = new ConcurrentHashMap[InetSocketAddress, ChannelHandlerContext]()
  def started(ctx: ChannelHandlerContext): Unit = {
    if(channelsRegistry.containsKey(ctx.channel().remoteAddress())) {
      throw new IllegalStateException(s"Already working with ${ctx.channel}")
    }
    channelsRegistry.put(ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress], ctx)
  }


  override def getName: String = s"TCP on $inetSocketAddress"

  override def sendNext(raw: RawData): Unit = {
    if(raw.getInetSocketAddress == null)
      throw new NullPointerException
    val ctx = channelsRegistry.get(raw.getInetSocketAddress)
    ctx.writeAndFlush(raw)
  }

  override def receiveNext(): RawData = inboundMessageQueue.poll()
}

class CoapTcpServer(inetAddress: InetSocketAddress) {
  def stopped(ctx: ChannelHandlerContext) = connector.stopped(ctx)

  def toCoap(a: InetSocketAddress): String =
    s"coap://${a.getHostString}:${a.getPort}"

  val logger = InternalLoggerFactory.getInstance(classOf[CoapTcpServer])
  def message(data: RawData): Unit = connector.message(data)

  def started(ctx: ChannelHandlerContext): Unit = {
    val remote = ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress]
    connector.started(ctx)
    val client = new CoapClient(toCoap(remote))
    client.setEndpoint(coapEndpoint)
    val get = Request.newGet()
    client.advanced(WriteToConsole, get)
    get.cancel()
//    client.observe(WriteToConsole)
  }

  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup()
  val socketAcceptor = new AcceptListener
  val connector = new TcpConnector(inetAddress)
  val coapEndpoint = new CoapEndpoint(connector, NetworkConfig.getStandard)
  def start: Unit = {
    val b = new ServerBootstrap()
    try {
      doStart(b)
    } finally {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }

  def doStart(b: ServerBootstrap): Unit = {
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(socketAcceptor)
      .childHandler(new CoapTcpChannelInitializer(this))

    val waitToStart = b.bind(inetAddress).sync()
    connector.start()
    logger.info("Started server and connector")
    waitToStart.channel().closeFuture().sync()
  }
}

class AcceptListener extends LoggingHandler(LogLevel.WARN) {

}

class CoapTcpChannelInitializer(parent: CoapTcpServer) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    ch.pipeline()
      .addLast(new ReadTimeoutHandler(1, TimeUnit.SECONDS))
      .addLast(new RawDataCodec)
      .addLast(new LoggingHandler(LogLevel.INFO))
      .addLast(new CoapChannelHandler(parent))
  }
}

class CoapChannelHandler(parent: CoapTcpServer) extends ChannelInboundHandlerAdapter {
  val logger = InternalLoggerFactory.getInstance(classOf[CoapChannelHandler])
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    logger.info(s"Notifying father about started ${ctx.channel()}")
    parent.started(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    super.channelRead(ctx, msg)
    parent.message(msg.asInstanceOf[RawData])
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    parent.stopped(ctx)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    parent.stopped(ctx)
    super.channelInactive(ctx)
  }
}



