package mass.job.util

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Path, StandardCopyOption}
import java.time.OffsetDateTime
import java.util.zip.ZipFile

import com.typesafe.scalalogging.StrictLogging
import helloscala.common.Configuration
import helloscala.common.exception.HSBadRequestException
import helloscala.common.util.{DigestUtils, FileUtils, Utils}
import mass.core.job.JobConstants
import mass.job.JobSettings
import mass.job.model.JobUploadJobReq
import mass.message.job._
import mass.model.job.{JobItem, JobTrigger, Program, TriggerType}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object JobUtils extends StrictLogging {

  case class JobZipInternal private (configs: Vector[JobConfig], entries: Vector[Path])

  def uploadJob(settings: JobSettings, req: JobUploadJobReq)(implicit ec: ExecutionContext): Future[JobZip] = Future {
    val sha256 = DigestUtils.sha256Hex(req.file.toPath)
    val dest = settings.jobSavedDir.resolve(sha256.take(2)).resolve(sha256)

    val jobZipInternal = parseJobZip(req.file, req.charset, dest.resolve(JobConstants.DIST)) match {
      case Right(v) => v
      case Left(e)  => throw e
    }

    val zipPath = dest.resolve(req.fileName)
    Files.move(req.file.toPath, zipPath, StandardCopyOption.REPLACE_EXISTING)
    JobZip(zipPath, jobZipInternal.configs, jobZipInternal.entries)
  }

  def parseJobZip(file: File, charset: Charset, dest: Path): Either[Throwable, JobZipInternal] = Utils.either {
    import scala.collection.JavaConverters._
    import scala.language.existentials

    val zip = new ZipFile(file, charset)
    try {
      val (confEntries, fileEntries) = zip
        .entries()
        .asScala
        .filterNot(entry => entry.isDirectory)
        .span(entry => entry.getName.endsWith(JobConstants.ENDS_SUFFIX) && !entry.isDirectory)
      val configs =
        confEntries.map(confEntry =>
          parseJobConf(FileUtils.getString(zip.getInputStream(confEntry), charset, "\n")) match {
            case Right(config) => config
            case Left(e)       => throw e
        })

      val buf = Array.ofDim[Byte](1024)
      val entryPaths = fileEntries.map { entry =>
        val entryName = entry.getName
        val savePath = dest.resolve(entryName)
        if (!Files.isDirectory(savePath.getParent)) {
          Files.createDirectories(savePath.getParent)
        }
        FileUtils.write(zip.getInputStream(entry), Files.newOutputStream(savePath), buf) // zip entry存磁盘
        savePath
      }

      JobZipInternal(configs.toVector, entryPaths.toVector)
    } finally {
      if (zip ne null) zip.close()
    }
  }

  def parseJobConf(content: String): Either[Throwable, JobConfig] = Utils.either {
    val now = OffsetDateTime.now()
    val conf = Configuration.parseString(content)

    val program = Program.fromName(conf.getString("detail.program").toUpperCase()).getOrElse(Program.UNKOWN)
    val programMain = conf.getString("detail.program-main")
    val _version = conf.getOrElse[String]("detail.program-version", "")
    val programVersion = ProgramVersion
      .get(program, _version)
      .getOrElse(throw HSBadRequestException(s"program-version: ${_version} 无效"))
    val jobItem = JobItem(
      conf.getString("detail.key"),
      program,
      if (conf.has("detail.program-options")) conf.get[Seq[String]]("detail.program-options") else Nil,
      programMain,
      if (conf.has("detail.program-args")) conf.get[Seq[String]]("detail.program-args") else Nil,
      programVersion.VERSION
    )

    val maybeTrigger = if (conf.has("trigger")) {
      val triggerType =
        TriggerType
          .fromName(conf.getString("trigger.trigger-type").toUpperCase())
          .getOrElse(TriggerType.TRIGGER_UNKNOWN)
      Some(
        JobTrigger(
          conf.getString("trigger.key"),
          triggerType,
          conf.getOrElse[String]("trigger.trigger-event", ""),
          conf.get[Option[OffsetDateTime]]("trigger.start-time"),
          conf.get[Option[OffsetDateTime]]("trigger.end-time"),
          conf.getOrElse[Int]("trigger.repeat", 0),
          conf.get[FiniteDuration]("trigger.duration"),
          conf.getOrElse[String]("trigger.cron-express", ""),
          conf.getOrElse[String]("trigger.description", ""),
          now
        ))
    } else None

    JobConfig(Some(jobItem), maybeTrigger)
  }

}

case class JobZip(zipPath: Path, configs: Vector[JobConfig], entries: Vector[Path])
