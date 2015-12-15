import java.net.{SocketAddress, InetSocketAddress}
import java.util
import java.util.concurrent.{ConcurrentLinkedQueue, ConcurrentHashMap}

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioSocketChannel, NioServerSocketChannel}
import io.netty.channel._
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.internal.logging.InternalLoggerFactory
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapResource, CoapServer}
import org.eclipse.californium.elements.{RawDataChannel, ConnectorBase, RawData}

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

class Server extends CoapServer {
  add(new HelloEndpoint)
}

class HelloEndpoint extends CoapResource("helloWorld") {
  getAttributes.setTitle("Hello world resource")

  override def handleGET(exchange: CoapExchange): Unit = {
    exchange.respond("Hello, World")
  }
}

class TcpConnector(inetSocketAddress: InetSocketAddress) extends ConnectorBase(inetSocketAddress) {
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
  val logger = InternalLoggerFactory.getInstance(classOf[CoapTcpServer])
  def message(data: RawData): Unit = connector.message(data)

  def started(ctx: ChannelHandlerContext): Unit = {
    val hello = new RawData("hello".getBytes, ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress])
    connector.started(ctx)
    connector.send(hello)
  }

  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup()
  val socketAcceptor = new AcceptListener
  val connector = new TcpConnector(inetAddress)

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
//    ctx.writeAndFlush(hello)
//    ctx.close()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    super.channelRead(ctx, msg)
    parent.message(msg.asInstanceOf[RawData])
  }
}

object RawDataCodec {
  private val logger = InternalLoggerFactory.getInstance(classOf[RawDataCodec])
}

class RawDataCodec extends ByteToMessageCodec[RawData] {
  import RawDataCodec._
  override def encode(ctx: ChannelHandlerContext, msg: RawData, out: ByteBuf): Unit = {
    val size = msg.getSize
    out.writeBytes("%02X".format(size).getBytes())
    out.writeBytes(msg.getBytes)
  }

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if(!in.isReadable(2))
      return
    in.markReaderIndex()
    val bytes = new Array[Byte](2)
    in.readBytes(bytes)
    val strSize = bytes.map{_.toChar}.mkString
    val size = Integer.parseInt(strSize)
    logger.info(s"Size is $strSize, $size")
    if(!in.isReadable(size)) {
      in.resetReaderIndex()
      return
    }
    val buf = new Array[Byte](size)
    in.readBytes(buf)
    val res = new RawData(buf, ctx.channel().asInstanceOf[SocketChannel].remoteAddress())
    val data = buf.map(_.toChar).mkString
    logger.info(s"Received data: $data")
    out.add(res)
  }
}

