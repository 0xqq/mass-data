package mass.job.business.job

import java.nio.file.Paths

import com.typesafe.scalalogging.StrictLogging
import mass.core.job._
import mass.job.JobSystem
import mass.job.business.components.JobRun
import mass.job.model.JobItems
import mass.message.job.SchedulerJobResult

import scala.concurrent.Future

class DefaultSchedulerJob extends SchedulerJob with StrictLogging {

  override def run(ctx: SchedulerContext): Future[JobResult] = {
    val jobSystem = ctx.scheduler.asInstanceOf[JobSystem]
    ctx.resources.get(JobItems.Resources.ZIP_PATH).map(handleZip(_, jobSystem, ctx)) getOrElse
      handle(jobSystem, ctx)
  }

  private def handleZip(
      zipPath: String,
      jobSystem: JobSystem,
      ctx: SchedulerContext
  ): Future[SchedulerJobResult] =
    Future {
      JobRun.runOnZip(Paths.get(zipPath), ctx.detailConfig, ctx.triggerConf.key, jobSystem.jobSettings)
    }(jobSystem.executionContext)

  private def handle(
      jobSystem: JobSystem,
      ctx: SchedulerContext
  ): Future[JobResult] =
    Future {
      JobRun.run(ctx.detailConfig, ctx.triggerConf.key, jobSystem.jobSettings)
    }(jobSystem.executionContext)

}
