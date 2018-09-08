package mass.core.job

import scala.concurrent.Future

trait SchedulerJob {

  def run(context: SchedulerContext): Future[JobResult]

}
