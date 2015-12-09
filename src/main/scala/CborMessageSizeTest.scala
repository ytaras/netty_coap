import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime

import co.nstant.in.cbor.CborEncoder
import co.nstant.in.cbor.builder.MapBuilder
import squants.storage.Megabytes
import squants.storage.StorageConversions._
import squants.time.TimeConversions._

/**
  * Created by ytaras on 12/9/15.
  */
object CborMessageSizeTest extends App {
  val doorOpenPeriod = 1.minutes
  val sensorPeriod = 30.seconds
  val sendPeriod = 1.minutes
  val sendCount: Int = (31.days / sendPeriod).toInt
  val sequence: Int = (sendPeriod / sensorPeriod).toInt
  val doorSequence: Int = (sendPeriod / doorOpenPeriod).toInt * 2
  val oneMessageSize = messageSize(sequence, doorSequence)
  val wholePayload = (oneMessageSize * sendCount).in(Megabytes)
  print(
    s"""
       |Door opens and closes every $doorOpenPeriod
       |We gather data from sensors every $sensorPeriod
       |We send data each $sendPeriod, which means we send $sequence sensor metrics and $doorSequence door events each time
       |We have 3 metrics - light, voltage, temp, which means we send ${3*sequence} measurements
       |Message size is $oneMessageSize
       |We send $sendCount messages per month which means $wholePayload payload
       |This does not include CoAP overhead AND server responses
     """.stripMargin)


  def messageSize(sequence: Int, doorSequence: Int) = {
    val result = Coder.generate(sequence, doorSequence)
    val baos = new ByteArrayOutputStream()
    new CborEncoder(baos).encode(result)
    baos.toByteArray.size.bytes
  }
}

object Coder {
  object KEYS {
    sealed abstract class key(val key: Long) {
      def toKey = key
    }
    case object timestamp extends key(1)
    case object temperature extends key(2)
    case object light extends key(3)
    case object door extends key(4)
    case object voltage extends key(5)
    case object id extends key(6)
    case object seq extends key(7)
    case object value extends key(8)
  }
  def generate(seq_size: Int, door_seq_size: Int) = {
    def auditMessage(builder: MapBuilder[_]) = {
      val map = builder
        .put(KEYS.timestamp.toKey, ZonedDateTime.now.toEpochSecond)
      seq(map, KEYS.temperature)
      seq(map, KEYS.light)
      seq(map, KEYS.voltage)
      door(map)
    }

    def seq(builder: MapBuilder[_], key: KEYS.key) = {
      val array = builder
        .putMap(key.toKey)
        .put(KEYS.id.toKey, Long.MaxValue)
        .putArray(KEYS.seq.toKey)
      (1 to seq_size) foreach { _ =>
        array.startMap()
          .put(KEYS.timestamp.toKey, ZonedDateTime.now.toEpochSecond)
          .put(KEYS.value.toKey, Integer.MAX_VALUE)
          .end()
      }
      array.end()
    }
    def door(builder: MapBuilder[_]) = {
      val arrayBuilder = builder
        .putMap(KEYS.door.toKey)
        .put(KEYS.id.toKey, Long.MaxValue)
        .putArray(KEYS.seq.toKey)
      (1 to door_seq_size) foreach { x =>
        arrayBuilder
          .startMap()
          .put(KEYS.timestamp.toKey, ZonedDateTime.now.toEpochSecond)
          .put(KEYS.value.toKey, false)
      }
    }


    val builder = new co.nstant.in.cbor.CborBuilder
    auditMessage(builder.startMap)
    builder.build()
  }
}