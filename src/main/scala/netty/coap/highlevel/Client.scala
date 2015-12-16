package netty.coap.highlevel

import org.eclipse.californium.core.CoapResponse
import org.eclipse.californium.core.coap.Request

import scala.concurrent.Future

/**
  * Created by ytaras on 12/16/15.
  */
trait Client {
  type Req = Request
  type Resp = CoapResponse
  def request(req: Req): Future[Resp]
}
