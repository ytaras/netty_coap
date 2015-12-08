import actors.Subscribe
import akka.actor.{Actor, ActorPath, Props}
import org.apache.spark.streaming.receiver.ActorHelper
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object SparkEtl {
  def start(remoteActor: ActorPath) = {
    val stream = ssc.actorStream[String](Props(classOf[DataReceiver], remoteActor), "StringToSparkReceiver")
    stream.print()
    ssc.start()
  }
  val conf = new SparkConf().setAppName("Speed layer").setMaster("local[*]")
  val sc = new SparkContext(conf)
  val ssc = new StreamingContext(sc, Seconds(1))
}

class DataReceiver(remoteActorRef: ActorPath) extends Actor with ActorHelper {
  override def preStart = context.actorSelection(remoteActorRef) ! Subscribe
  override def receive = {
    case x => store(x)
  }
}
