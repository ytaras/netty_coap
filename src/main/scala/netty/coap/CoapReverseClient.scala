package netty.coap

/**
  * Created by ytaras on 12/16/15.
  */
trait CoapReverseClient[A] {
  def newClientActive(client: Client[A]): Unit
  def start(): Unit
  def stop(): Unit
}






