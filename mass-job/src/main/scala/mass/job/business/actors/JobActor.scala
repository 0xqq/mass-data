package mass.job.business.actors

import java.time.OffsetDateTime

import akka.actor.{Actor, Props}
import akka.pattern._
import com.typesafe.scalalogging.StrictLogging
import mass.message.job._
import mass.job.JobSystem
import mass.job.business.job.DefaultSchedulerJob
import mass.job.model._
import mass.job.repository.JobRepo
import mass.job.util.JobUtils

import scala.concurrent.{ExecutionContext, Future}

object JobActor {
  val job = 'job

  @inline def jobMessage(msg: JobMessage): (Symbol, JobMessage) = job -> msg

  def props(jobSystem: JobSystem): (Props, Symbol) = Props(new JobActor(jobSystem)) -> job
}

class JobActor(val jobSystem: JobSystem) extends Actor with JobService {
  import context.dispatcher

  override def receive: Receive = {
    case req: JobPageReq           => handleJobPage(req).pipeTo(sender())
    case req: JobUploadJobReq      => handleUploadJob(req).pipeTo(sender())
    case req: JobUpdateTriggerReq  => handleUpdateTrigger(req).pipeTo(sender())
    case req: JobListJobItemReq    => handleGetItem(req).pipeTo(sender())
    case req: JobListJobTriggerReq => handleGetTrigger(req).pipeTo(sender())
  }

}

trait JobService extends StrictLogging {

  val jobSystem: JobSystem

  private val db = jobSystem.massSystem.sqlManager

  def handleGetItem(req: JobListJobItemReq)(implicit ec: ExecutionContext): Future[JobListJobItemResp] = {
    db.run(JobRepo.listJobItem(req)).map(list => JobListJobItemResp(list.map(_.config)))
  }

  def handleGetTrigger(req: JobListJobTriggerReq)(implicit ec: ExecutionContext): Future[JobListJobTriggerResp] = {
    db.run(JobRepo.listJobTrigger(req)).map(list => JobListJobTriggerResp(list.map(_.config)))
  }

  def handleJobPage(req: JobPageReq)(implicit ec: ExecutionContext): Future[JobPageResp] = db.run(JobRepo.page(req))

  def handleUploadJob(req: JobUploadJobReq)(implicit ec: ExecutionContext): Future[JobUploadJobResp] =
    JobUtils
      .uploadJob(jobSystem.jobSettings, req)
      .flatMap(jobZip => db.runTransaction(JobRepo.save(jobZip)(db.executionContext)))
      .map { list =>
        val results = list.map {
          case (jobItem, Some(jobTrigger)) =>
            val createTime = jobSystem.schedulerJob(jobItem.config,
                                                    jobTrigger,
                                                    classOf[DefaultSchedulerJob].getName,
                                                    Some(jobItem.config.data))
            JobScheduleResult(jobItem.key, jobTrigger.key, createTime.toInstant.toEpochMilli)
          case (jobItem, _) => JobScheduleResult(jobItem.key)
        }
        JobUploadJobResp(results)
      }

  def handleUpdateTrigger(req: JobUpdateTriggerReq): Future[JobUpdateTriggerResp] = {
    implicit val ec = db.executionContext
    val triggerConfig = req.config.get
    // TODO creator？是否需要updater
    val payload = JobTriggerRow(req.triggerKey, triggerConfig, "", OffsetDateTime.now())
    db.runTransaction(JobRepo.saveJobTrigger(payload))
      .flatMap { maybe =>
        db.run(JobRepo.listJobItem(JobListJobItemReq(triggerKey = req.triggerKey)))
          .map(schedules => maybe.get -> schedules)
      }
      .map {
        case (jobTrigger, jobItems) =>
          jobSystem.rescheduleJob(jobTrigger.config, jobItems.map(_.config), classOf[DefaultSchedulerJob].getName)
          JobUpdateTriggerResp(jobTrigger.config)
      }
  }

}
