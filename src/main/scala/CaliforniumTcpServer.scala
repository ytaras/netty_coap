import java.net.InetSocketAddress
import java.util

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.{ByteToMessageCodec, MessageToByteEncoder}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapResource, CoapServer}
import org.eclipse.californium.elements.RawData

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


class CoapTcpServer(inetAddress: InetSocketAddress) {
  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup()

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
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(new CoapTcpChannelInitializer)

    b.bind(inetAddress).sync().channel().closeFuture().sync()
  }
}

class CoapTcpChannelInitializer extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    println(s"Accepted connection ${ch.remoteAddress()}")
    ch.pipeline()
      .addLast(new RawDataCodec)
      .addLast(new LoggingHandler(LogLevel.DEBUG))
  }
}

class RawDataCodec extends ByteToMessageCodec[RawData] {
  override def encode(ctx: ChannelHandlerContext, msg: RawData, out: ByteBuf): Unit = {
    val size = msg.getSize
    out.writeShort(size)
    out.writeBytes(msg.getBytes)
  }

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if(!in.isReadable(2))
      return
    in.markReaderIndex()
    val size = in.readUnsignedShort()
    println(s"Read size is $size")
    if(!in.isReadable(size)) {
      println(s"${in.readableBytes()} avaiable, needed $size, return")
      in.resetReaderIndex()
      return
    }
    val buf = new Array[Byte](size)
    in.readBytes(buf)
    println(s"$size bytes read")
    val res = new RawData(buf, ctx.channel().asInstanceOf[SocketChannel].remoteAddress())
    val data = buf.map(_.toChar).mkString
    println(s"Raw data: $data")
    out.add(res)
  }
}

