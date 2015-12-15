package netty.codec.coap

import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.{MessageToByteEncoder, ReplayingDecoder}
import org.eclipse.californium.core.coap.{Response, Request, EmptyMessage, Message}
import org.eclipse.californium.core.network.serialization.{Serializer, DataParser}

/**
  * Created by ytaras on 12/15/15.
  */
class CoapMessageDecoder extends ReplayingDecoder[Void] {
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    val size = in.readUnsignedShort()
    val bytes = in.readBytes(size).array()
    val parser = new DataParser(bytes)
    out add parser.parseRequest()
  }
}

class CoapMessageEncoder extends MessageToByteEncoder[Message] {
  val serializer = new Serializer
  override def encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf): Unit = {
  val bytes = serialize(msg)
    out.writeShort(bytes.length.toShort)
    out.writeBytes(bytes)
  }

  def serialize(msg: Message): Array[Byte] = {
    val raw = msg match {
      case x: EmptyMessage => serializer.serialize(x)
      case x: Request => serializer.serialize(x)
      case x: Response => serializer.serialize(x)
    }
    raw.getBytes
  }

}
