import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.eclipse.californium.core.{CoapClient, CoapHandler, CoapResponse}

class Kafka {
  val props = new Properties()
  props.setProperty("bootstrap.servers", "localhost:9092")
  props.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  val producer: KafkaProducer[String, String] = new KafkaProducer(props)
}

object Main extends App {
  val kafka = new Kafka
  val handler = new CoapHandler {
    override def onError(): Unit = print("Error!")

    override def onLoad(response: CoapResponse): Unit = {
      val str = response.getPayload.map{_.toChar}.mkString
      kafka.producer.send(new ProducerRecord("topic", response.advanced().getSource.toString, response.getResponseText))
    }
  }

  def observe() {
    val client = new CoapClient("coap://vs0.inf.ethz.ch:5683/obs")
    client.observe(handler)

  }
  for {
    i <- 1 to 1000
  } observe()

  Spark
}

object Spark {
  val sparkConf = new SparkConf().setMaster("local[2]").setAppName("My cool app")
  val sc = new SparkContext(sparkConf)
  val ssc = new StreamingContext(sc, Seconds(2))
  val reader = KafkaUtils.createStream(ssc, "localhost:2181", "consumegroup", Map("topic" -> 1))
  reader.reduceByKey(_ + _).print
  ssc.start()
  ssc.awaitTermination()
}


