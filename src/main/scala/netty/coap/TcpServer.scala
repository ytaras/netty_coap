package netty.coap

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer, ChannelHandler}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.internal.logging.InternalLoggerFactory

/**
  * Created by ytaras on 12/16/15.
  */
trait TcpServer {
  val logger = InternalLoggerFactory.getInstance(getClass)
  private val bossGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()
  def bindTo: InetSocketAddress

  @Sharable
  class ChannelActiveListener extends ChannelInboundHandlerAdapter {
    override def channelActive(ctx: ChannelHandlerContext): Unit = {
      super.channelActive(ctx)
      onChannelActive(ctx)
    }
  }
  val channelActiveListener = new ChannelActiveListener

  val childHandler = new ChannelInitializer[SocketChannel] {
    override def initChannel(ch: SocketChannel): Unit = {
      ch
        .pipeline()
        .addLast(new LoggingHandler(getClass, LogLevel.DEBUG))
        .addLast(channelActiveListener)
      socketInit(ch)
    }
  }

  def stop(): Unit = {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }

  def start(): Unit = {
    val b = new ServerBootstrap()
    try {
      b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(parentHandler)
      .childHandler(childHandler)
      b.bind(bindTo).sync().channel().closeFuture().sync()
    } finally {
      stop()
    }
  }

  def parentHandler: ChannelHandler = new LoggingHandler(getClass, LogLevel.DEBUG)
  def onChannelActive(ctx: ChannelHandlerContext): Unit
  def socketInit(ch: SocketChannel): Unit
}
