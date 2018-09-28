package mass.extension

import java.nio.file.{Files, Path, Paths}

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import helloscala.common.Configuration

case class MassSettings(system: ExtendedActorSystem) extends Extension {
  private var _tempDirectory: Path = _

  val configuration = Configuration(system.settings.config)

  def name: String = configuration.getString("mass.name")

  def tempDirectory: Path = synchronized {
    _tempDirectory =
      Paths.get(configuration.getOrElse[String]("mass.core.temp-dir", System.getProperty("java.io.tmpdir")))
    if (!Files.isDirectory(_tempDirectory)) {
      Files.createDirectories(_tempDirectory)
    }
    _tempDirectory
  }

}

object MassSettings extends ExtensionId[MassSettings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MassSettings = new MassSettings(system)
  override def lookup(): ExtensionId[_ <: Extension] = MassSettings
}
