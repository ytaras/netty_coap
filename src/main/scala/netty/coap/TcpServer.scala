package netty.coap

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.internal.logging.InternalLoggerFactory
import netty.coap.highlevel.impl.RawDataCodec

/**
  * Created by ytaras on 12/15/15.
  */
class TcpServer(localAddress: InetSocketAddress) {
  val logger = InternalLoggerFactory.getInstance(getClass)

  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup()
  def start(): Unit = {
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
        .childHandler(new TcpChannelInitializer)

      val waitToStart = b.bind(localAddress).sync()
      logger.info("Started server and connector")
      waitToStart.channel().closeFuture().sync()
    }

}

class TcpChannelInitializer extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    ch.pipeline()
      .addLast(new LoggingHandler(LogLevel.INFO))
      .addLast(new RawDataCodec)
  }
}

class SocketCreationListener extends ChannelInboundHandlerAdapter {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
  }
}
