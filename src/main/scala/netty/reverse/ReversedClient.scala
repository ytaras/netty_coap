package netty.reverse

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelInitializer, ServerChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.logging.{LogLevel, LoggingHandler}

/**
  * Created by ytaras on 12/15/15.
  */
abstract class ReversedClient[C <: Channel](listen: InetSocketAddress, channel: Class[_ <: ServerChannel])
  extends ChannelInitializer[C] {
  val boss = new NioEventLoopGroup(1)
  val worker = new NioEventLoopGroup()
  val b = new ServerBootstrap()
  b.group(boss, worker)
    .channel(channel)
    .handler(new LoggingHandler(LogLevel.INFO))
    .childHandler(this)
  def start() = {
    try {
      b.bind(listen).sync()
    } finally {
      boss.shutdownGracefully()
      worker.shutdownGracefully()
    }
  }
}
