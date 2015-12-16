package netty.coap.highlevel

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
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
  private val logger = InternalLoggerFactory.getInstance(getClass)
  private val bossGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()
  def bindTo: InetSocketAddress
  val channelActiveListener = new ChannelInboundHandlerAdapter {
    override def channelActive(ctx: ChannelHandlerContext): Unit = {
      onChannelActive(ctx)
      super.channelActive(ctx)
    }
  }
  val childHandler = new ChannelInitializer[SocketChannel] {
    override def initChannel(ch: SocketChannel): Unit = {
      ch
        .pipeline()
        .addLast(new LoggingHandler(getClass, LogLevel.INFO))
        .addLast(channelActiveListener)
      socketInit(ch)
    }
  }

  def stop() = {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }

  def start() = {
    val b = new ServerBootstrap()
    try {
      b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(parentHandler)
      .childHandler(childHandler)
      b.bind(bindTo).sync()
    } finally {
      stop()
    }
  }

  def parentHandler: ChannelHandler = new LoggingHandler(getClass, LogLevel.INFO)
  def onChannelActive(ctx: ChannelHandlerContext): Unit
  def socketInit(ch: SocketChannel): Unit
}
