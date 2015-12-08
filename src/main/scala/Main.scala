import actors.CaliforniumServerActor
import akka.actor._
import org.apache.spark.SparkEnv
import sun.jvm.hotspot.debugger.remote.RemoteAddress

/**
  * Created by ytaras on 12/8/15.
  */
object Main extends App {

  val system = ActorSystem("CoapSystem")
  val server = system.actorOf(Props[CaliforniumServerActor])
//  SparkEtl.start(server.path.)
  val addr = RemoteAddressExtension(system).address
  val serverAddr = server.path.toStringWithAddress(addr)
  print(serverAddr)
  SparkEtl.start(serverAddr)
}
class RemoteAddressExtensionImpl(system: ExtendedActorSystem) extends Extension {
  def address = system.provider.getDefaultAddress
}
object RemoteAddressExtension extends ExtensionKey[RemoteAddressExtensionImpl]
