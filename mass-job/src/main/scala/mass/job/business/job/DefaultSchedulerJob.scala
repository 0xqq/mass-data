package mass.job.business.job

import java.nio.file.Paths

import com.typesafe.scalalogging.StrictLogging
import mass.core.job._
import mass.message.job.SchedulerJobResult
import mass.job.JobSystem
import mass.job.business.components.JobRun
import mass.job.model.JobItems

import scala.concurrent.{ExecutionContext, Future}

class DefaultSchedulerJob extends SchedulerJob with StrictLogging {

  def handleZip(
      zipPath: String,
      jobSystem: JobSystem,
      ctx: SchedulerContext
  ): Future[SchedulerJobResult] = {
    implicit val ec: ExecutionContext = jobSystem.executionContext
    JobRun.runOnZip(Paths.get(zipPath), ctx.detailConfig, ctx.triggerConf.key, jobSystem.jobSettings)
  }

  def handle(
      jobSystem: JobSystem,
      ctx: SchedulerContext
  ): Future[JobResult] = {
    implicit val ec: ExecutionContext = jobSystem.executionContext
    JobRun.run(ctx.detailConfig, ctx.triggerConf.key, jobSystem.jobSettings)
  }

  override def run(ctx: SchedulerContext): Future[JobResult] = {
    val jobSystem = ctx.scheduler.asInstanceOf[JobSystem]

    ctx.resources.get(JobItems.Resources.ZIP_PATH).map(handleZip(_, jobSystem, ctx)) getOrElse
      handle(jobSystem, ctx)
  }

}
