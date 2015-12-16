package netty.coap.sample

import java.net.InetSocketAddress

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.internal.logging.InternalLoggerFactory
import netty.coap.impl.RawDataCodec
import org.eclipse.californium.core.coap.CoAP.{Type, ResponseCode}
import org.eclipse.californium.core.coap.Response
import org.eclipse.californium.core.network.serialization.{DataParser, Serializer}
import org.eclipse.californium.elements.RawData

/**
  * Created by ytaras on 12/16/15.
  */
object SampleReverseServerApp extends App {
  new SampleReverseServer(new InetSocketAddress(1234)).start()
}

class SampleReverseServer(connectTo: InetSocketAddress) {
  val workerGroup = new NioEventLoopGroup()
  val b = new Bootstrap()
  b.group(workerGroup)
    .channel(classOf[NioSocketChannel])
    .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
  b.handler(new ChannelInitializer[SocketChannel] {
    override def initChannel(ch: SocketChannel): Unit = {
      ch.pipeline()
        .addLast(new LoggingHandler(LogLevel.INFO))
        .addLast(new RawDataCodec)
        .addLast(PrintRawData)
    }
  })
  def start() = {
    b.connect(connectTo).sync()
      .channel().closeFuture().sync()
  }
}

object PrintRawData extends SimpleChannelInboundHandler[RawData] {
  val logger = InternalLoggerFactory.getInstance(getClass)
  val serializer = new Serializer

  override def channelRead0(ctx: ChannelHandlerContext, msg: RawData): Unit = {
    val byteAsStr = msg.bytes.map { "%02X" format _ }.mkString
    logger.info(s"Received bytes: $byteAsStr")
    val parser = new DataParser(msg.getBytes)
    logger.info(s"Parser: $parser")
    val req = parser.parseRequest()
    logger.info(s"received req $req")
    val resp = Response.createResponse(req, ResponseCode.CONTENT)
    resp.setToken(req.getToken)
    resp.setPayload("Some payload")
    resp.setType(Type.ACK)
    resp.setMID(req.getMID)
    ctx.channel().writeAndFlush(serializer.serialize(resp))

  }

}
