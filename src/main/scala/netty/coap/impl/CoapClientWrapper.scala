package netty.coap.impl

import netty.coap.Client
import org.eclipse.californium.core.CoapClient

import scala.concurrent.{Future, Promise}

/**
  * Created by ytaras on 12/16/15.
  */
class CoapClientWrapper(coapClient: CoapClient) extends Client {
  override def request(req: Req): Future[Resp] = {
    val p = Promise[Resp]()
    coapClient.advanced(new PromiseHandler(p), req)
    p.future
  }
}

class UnknownCoapException extends RuntimeException
