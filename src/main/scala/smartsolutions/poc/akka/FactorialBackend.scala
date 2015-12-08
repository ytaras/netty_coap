package smartsolutions.poc.akka

import akka.actor.Actor.Receive
import akka.actor.{ReceiveTimeout, ActorLogging, Actor}
import akka.pattern.pipe
import akka.routing.FromConfig

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by ytaras on 12/8/15.
  */
class FactorialBackend extends Actor with ActorLogging {
  import context.dispatcher

  def receive = {
    case n: Int =>
      Future(factorial(n)) map { (n, _) } pipeTo sender()
    case x => log.warning(s"Unknown message $x")
  }

  def factorial(n: Int): BigInt = {
    @tailrec def factorialAcc(acc: BigInt, n: Int): BigInt = {
      if (n <= 1) acc
      else factorialAcc(acc * n, n - 1)
    }
    factorialAcc(BigInt(1), n)
  }

}

class FactorialFrontend(upToN: Int, repeat: Boolean) extends Actor with ActorLogging {
  val backend = context.actorOf(FromConfig.props, name = "factorialBackendRouter")
  override def preStart = {
    context.setReceiveTimeout(10.seconds)
  }

  def sendJobs() = {
    log.info(s"Starting batch of factorials up to $upToN")
    1 to upToN foreach { backend ! _ }
    log.info(s"Sent batch of factorials up to $upToN")
  }

  override def receive: Receive = {
    case (n: Int, factorial: BigInt) =>
      if ( n == upToN) {
        log.info(s"$n! = $factorial")
        if (repeat) sendJobs()
        else context.stop(self)
      }
    case "start" => sendJobs()
    case ReceiveTimeout =>
      log.info("Timeout")
      sendJobs()
    case x => log.warning(s"Unknown message $x")
  }
}
