package netty.coap.highlevel.impl

import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.util.internal.logging.InternalLoggerFactory
import org.eclipse.californium.core.network.serialization.DataParser
import org.eclipse.californium.elements.RawData

/**
  * Created by ytaras on 12/15/15.
  */
class RawDataCodec extends ByteToMessageCodec[RawData] {
  private val logger = InternalLoggerFactory.getInstance(classOf[RawDataCodec])
  override def encode(ctx: ChannelHandlerContext, msg: RawData, out: ByteBuf): Unit = {
    val size = msg.getSize
    val p = new DataParser(msg.getBytes)
    logger.info(s"Writing ${p.parseRequest()}")
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
