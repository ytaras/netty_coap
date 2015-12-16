package netty.coap.impl

import org.eclipse.californium.core.{CoapHandler, CoapResponse}

import scala.concurrent.Promise

/**
  * Created by ytaras on 12/16/15.
  */
class PromiseHandler(p: Promise[CoapResponse]) extends CoapHandler {
  override def onError(): Unit = p.failure(new UnknownCoapException())

  override def onLoad(response: CoapResponse): Unit = p.success(response)
}
