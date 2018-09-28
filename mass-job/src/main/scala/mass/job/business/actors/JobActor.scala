package mass.job.business.actors

import java.time.OffsetDateTime

import akka.actor.{Actor, Props}
import akka.pattern._
import com.typesafe.scalalogging.StrictLogging
import mass.core.protobuf.ProtoUtils
import mass.job.JobSystem
import mass.job.business.job.DefaultSchedulerJob
import mass.job.model._
import mass.job.repository.JobRepo
import mass.job.util.{JobUtils, ProgramVersion}
import mass.message.job.JobGetAllOptionResp.ProgramVersionItem
import mass.message.job._
import mass.model.job._
import mass.model.{CommonStatus, TitleValue}

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
    case req: JobGetAllOptionReq   => handleGetAllOption(req).pipeTo(sender())
    case req: JobCreateReq         => handleCreateJob(req).pipeTo(sender())
  }

}

trait JobService extends StrictLogging {

  val jobSystem: JobSystem

  private val db = jobSystem.massExSystem.sqlManager

  def handleGetItem(req: JobListJobItemReq)(implicit ec: ExecutionContext): Future[JobListJobItemResp] = {
    db.run(JobRepo.listJobItem(req)).map(list => JobListJobItemResp(list.map(_.config.get)))
  }

  def handleGetTrigger(req: JobListJobTriggerReq)(implicit ec: ExecutionContext): Future[JobListJobTriggerResp] = {
    db.run(JobRepo.listJobTrigger(req)).map(list => JobListJobTriggerResp(list.map(_.config.get)))
  }

  def handleJobPage(req: JobPageReq)(implicit ec: ExecutionContext): Future[JobPageResp] = db.run(JobRepo.page(req))

  def handleUploadJob(req: JobUploadJobReq)(implicit ec: ExecutionContext): Future[JobUploadJobResp] =
    JobUtils
      .uploadJob(jobSystem.jobSettings, req)
      .flatMap(jobZip => db.runTransaction(JobRepo.save(jobZip)(db.executionContext)))
      .map { list =>
        val results = list.map {
          case (jobItemRow, Some(jobTrigger)) =>
            val jobItem = jobItemRow.config.get
            val createTime = schedulerJob(jobItem, jobTrigger, jobItem.data)
            JobScheduleResult(jobItemRow.key, Some(jobTrigger.key), Some(createTime))
          case (jobItem, _) => JobScheduleResult(jobItem.key)
        }
        JobUploadJobResp(results)
      }

  def handleUpdateTrigger(req: JobUpdateTriggerReq): Future[JobUpdateTriggerResp] = {
    implicit val ec = db.executionContext
    // TODO creator？是否需要updater
    val payload = JobTriggerRow(req.triggerKey, req.trigger, "", OffsetDateTime.now())
    db.runTransaction(JobRepo.saveJobTrigger(payload))
      .flatMap { maybe =>
        db.run(JobRepo.listJobItem(JobListJobItemReq(triggerKey = req.triggerKey)))
          .map(schedules => maybe.get -> schedules)
      }
      .map {
        case (jobTrigger, jobItems) =>
          jobSystem.rescheduleJob(jobTrigger.config.get,
                                  jobItems.map(_.config.get),
                                  classOf[DefaultSchedulerJob].getName)
          JobUpdateTriggerResp(Some(jobTrigger.config.get))
      }
  }

  def handleGetAllOption(req: JobGetAllOptionReq)(implicit ec: ExecutionContext): Future[JobGetAllOptionResp] = Future {
    val programs = ProtoUtils.enumToTitleIdValues(Program)
    val triggerType = ProtoUtils.enumToTitleIdValues(TriggerType)
    val programVersion = ProgramVersion.values
      .groupBy(_.NAME)
      .map {
        case (program, versions) =>
          ProgramVersionItem(program.value, versions.map(p => TitleValue(p.VERSION, p.VERSION)))
      }
      .toList
    val jobStatus = ProtoUtils.enumToTitleIdValues(JobStatus)
    JobGetAllOptionResp(programs, triggerType, programVersion, jobStatus)
  }

  def handleCreateJob(req: JobCreateReq)(implicit ec: ExecutionContext): Future[JobGetScheduleResp] = {
    db.runTransaction(JobRepo.save(req.item.get, req.trigger, req.description)).map { _ =>
      req.trigger.foreach(trigger => schedulerJob(req.item.get, trigger))
      JobGetScheduleResp(req.item, req.trigger, CommonStatus.Continue)
    }
  }

  private def schedulerJob(config: JobItem, trigger: JobTrigger, data: Map[String, String] = Map()): OffsetDateTime = {
    jobSystem.schedulerJob(config,
                           trigger,
                           classOf[DefaultSchedulerJob].getName,
                           Some(if (data.isEmpty) config.data else data))
  }

}
