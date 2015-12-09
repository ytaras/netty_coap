import akka.actor.{Actor, Props, ActorSystem, ActorRef}
import squants.storage.StorageConversions._

/**
  * Created by ytaras on 12/8/15.
  */
object MemoryTest extends App {
  System.gc()
  var map: Map[String, ActorRef] = Map()
  val system = ActorSystem("Actors")
  val actor = system.actorOf(Props[NoOpActor])
  val re = withUsedMemoryDelta {
    for {
      i <- 1 to 10000000
    } map += (s"coap://url${i}/some/url/long" -> actor)
  }
  print(re)

  def getUsedMemory = {
    System.gc()
    Runtime.getRuntime.totalMemory.bytes.toMegabytes.megabytes
  }

  def withUsedMemoryDelta(block: => Unit) = {
    val mem1 = getUsedMemory
    block
    val mem2 = getUsedMemory
    (mem1, mem2, mem2 - mem1)
  }
}

class NoOpActor extends Actor {
  override def receive: Receive = {
    case x => print(x)
  }
}

