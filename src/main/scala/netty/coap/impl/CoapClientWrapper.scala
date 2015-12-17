package netty.coap.impl

import io.netty.channel.ChannelHandlerContext
import netty.coap.Client
import org.eclipse.californium.core.CoapClient

import scala.concurrent.{Future, Promise}

/**
  * Created by ytaras on 12/16/15.
  */
class CoapClientWrapper(val underlyingClient: CoapClient, ctx: ChannelHandlerContext) extends Client[CoapClient] {
  override def request(req: Req): Future[Resp] = {
    val p = Promise[Resp]()
    underlyingClient.advanced(new PromiseHandler(p), req)
    p.future
  }
  override def close(): Unit = ctx.close()
}

class UnknownCoapException extends RuntimeException
