package smartsolutions.poc.akka

import akka.actor.{Actor, ActorLogging}

/**
  * Created by ytaras on 12/7/15.
  */
class SampleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case x => log.info(s"Received string ${x}")
  }
}
