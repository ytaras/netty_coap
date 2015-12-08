import actors.Subscribe
import akka.actor.{ActorRef, Actor, Props}
import org.apache.spark.streaming.receiver.ActorHelper
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

object SparkEtl {
  def start(remoteActor: String, publishResultTo: ActorRef) = {
    val stream = ssc.actorStream[String](Props(classOf[DataReceiver], remoteActor), "StringToSparkReceiver")
    val result = stream.flatMap { _ split "\\s+" } map { (_, 1) } reduceByKey { _ + _ }
    result.foreachRDD { rdd =>
      publishResultTo ! s"Time: ${System.currentTimeMillis()}, result is ${rdd.collect().toList.mkString}"
    }
    ssc.start()
  }
  val conf = new SparkConf().setAppName("Speed layer").setMaster("local[*]")
  val sc = new SparkContext(conf)
  val ssc = new StreamingContext(sc, Seconds(30))
}

class DataReceiver(remoteActorRef: String) extends Actor with ActorHelper {
  override def preStart = context.actorSelection(remoteActorRef) ! Subscribe
  override def receive = {
    case x => store(x)
  }
}
