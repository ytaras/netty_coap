package netty.coap.impl

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
    logger.debug(s"Sending message $p")
    out.writeShort(size)
    out.writeBytes(msg.getBytes)
  }

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if(!in.isReadable(2))
      return
    in.markReaderIndex()
    val size = in.readUnsignedShort()
    if (size == 0)
      throw new IllegalStateException("Message size can't be 0")
    if(!in.isReadable(size)) {
      in.resetReaderIndex()
      return
    }
    val buf = new Array[Byte](size)
    in.readBytes(buf)
    val res = new RawData(buf, ctx.channel().asInstanceOf[SocketChannel].remoteAddress())
    val data = buf.map(_.toChar).mkString
    logger.debug(s"Received data: $data, message ${new DataParser(buf)}")
    out.add(res)
  }
}
