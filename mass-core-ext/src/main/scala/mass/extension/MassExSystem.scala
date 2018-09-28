package mass.extension

import java.nio.file.Path

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import helloscala.common.Configuration
import mass.slick.SqlManager

class MassExSystem(system: ExtendedActorSystem) extends Extension {
  val settings = MassSettings(system)

  val sqlManager: SqlManager = {
    val m = SqlManager(settings.configuration)
    sys.addShutdownHook(m.slickDatabase.close())
    m
  }

  def connection: Configuration = settings.configuration
  def tempDirectory: Path = settings.tempDirectory
}

object MassExSystem extends ExtensionId[MassExSystem] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MassExSystem = new MassExSystem(system)
  override def lookup(): ExtensionId[_ <: Extension] = MassExSystem
}
