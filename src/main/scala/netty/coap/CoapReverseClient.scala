package netty.coap

/**
  * Created by ytaras on 12/16/15.
  */
trait CoapReverseClient {
  def newClientActive(client: Client): Unit
  def start(): Unit
  def stop(): Unit
}






