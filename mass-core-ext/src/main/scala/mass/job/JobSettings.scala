package mass.job

import java.nio.file.{Path, Paths}

import helloscala.common.Configuration
import mass.server.MassConfig

case class JobSettings(configuration: Configuration) {
  val massConfig = MassConfig(configuration)

  private val conf = configuration.getConfiguration("mass.job")

  def jobSavedDir: Path =
    conf
      .get[Option[Path]]("job-saved-dir")
      .getOrElse(Paths.get(sys.props.getOrElse("user.dir", ""), "share", "job-saved"))

  def jobRunDir: Path = Paths.get(sys.props.getOrElse("user.dir", ""), "share", "job-run")

  def schedulerRunJar: String = ""
}
