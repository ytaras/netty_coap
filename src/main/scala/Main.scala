import akka.actor._
import coap.CaliforniumServer
import org.eclipse.californium.core.{CoapResponse, CoapHandler, CoapClient}
import org.eclipse.californium.core.coap.MediaTypeRegistry
import scala.concurrent.duration._

/**
  * Created by ytaras on 12/8/15.
  */
object Main extends App {

  val system = ActorSystem("CoapSystem")
  val californiumServer = new CaliforniumServer(system)
  val dataStream = californiumServer.dataStreamActor
  val serverAddr = dataStream.path.toStringWithAddress(RemoteAddressExtension(system).address)
  SparkEtl.start(serverAddr, californiumServer.resultsActor)
  Thread.sleep(30.seconds.toMillis)
  Client.run()
}

object Client {
  def run(): Unit = {
    val resultClient = new CoapClient("coap://10.128.194.202:5683/results")
    resultClient.observe(new CoapHandler {
      override def onError(): Unit = {
        print("ERRROROR")
        System.exit(-1)
      }

      override def onLoad(response: CoapResponse): Unit = {
        print("Result!!!")
        print(response.getPayload.map(_.toChar).mkString)
      }
    })

    val valueClient = new CoapClient("coap://10.128.194.202:5683/helloWorld")
    while(true) {
      valueClient.put("Text Text duplicated", MediaTypeRegistry.TEXT_PLAIN)
      Thread.sleep(10)
    }

  }
}

class RemoteAddressExtensionImpl(system: ExtendedActorSystem) extends Extension {
  def address = system.provider.getDefaultAddress
}

object RemoteAddressExtension extends ExtensionKey[RemoteAddressExtensionImpl]
