package mass.job

import java.time.OffsetDateTime

import com.typesafe.scalalogging.StrictLogging
import helloscala.common.exception.{HSException, HSNotFoundException}
import helloscala.common.types.ObjectId
import helloscala.common.util.StringUtils
import mass.core.job.{JobConstants, SchedulerContext, SchedulerJob}
import mass.job.repository.JobRepo
import mass.model.job.{JobLog, JobStatus}
import org.quartz.{Job, JobExecutionContext}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[job] class JobClassJob extends Job with StrictLogging {

  override def execute(context: JobExecutionContext): Unit = {
    val jobClass = context.getJobDetail.getJobDataMap.getString(JobConstants.JOB_CLASS)
    require(StringUtils.isNoneBlank(jobClass), s"JOB_CLASS 不能为空。")

    val db = JobSystem.instance.massSystem.sqlManager
    val jobKey = context.getJobDetail.getKey.getName
    val triggerKey = context.getTrigger.getKey.getName
    val logId = ObjectId.getString()
    val now = OffsetDateTime.now()
    val log = JobLog(logId, jobKey + triggerKey, jobKey, triggerKey, now, None, JobStatus.JOB_RUNNING, None, now)
    db.runTransaction(JobRepo.insertJobLog(log))

    val clz = Class.forName(jobClass)
    if (classOf[SchedulerJob].isAssignableFrom(clz)) {
      implicit val ec: ExecutionContext = JobSystem.instance.executionContext
      db.run(JobRepo.findJob(jobKey, triggerKey))
        .flatMap {
          case Some((jobItem, jobTrigger)) =>
            val data = (context.getJobDetail.getJobDataMap.asScala.mapValues(_.toString) - JobConstants.JOB_CLASS).toMap
            val config = jobItem.config.get
            val ctx = SchedulerContext(config, jobTrigger.config.get, data, config.resources, JobSystem.instance)
            clz.newInstance().asInstanceOf[SchedulerJob].run(ctx)
          case _ => Future.failed(HSNotFoundException(s"Job[$jobKey:$triggerKey]未找到"))
        }
        .map { result =>
          val msg = s"调度任务执行成功：$result。"
          logger.info(msg)
          JobStatus.JOB_OK -> msg
        }
        .recover {
          case e: HSException =>
            JobStatus.JOB_FAILURE -> e.getErrMsg
          case e => JobStatus.JOB_FAILURE -> s"调度任务执行错误：${e.toString}。"
        }
        .foreach {
          case (status, msg) =>
            if (status != JobStatus.JOB_OK) {
              logger.error(msg)
            }
            status -> msg
            db.runTransaction(JobRepo.completionJobLog(logId, OffsetDateTime.now(), status, msg))
        }
    } else {
      val msg = s"未知的任务类型：${clz.getName}，需要 ${classOf[SchedulerJob].getName} 的子类。"
      logger.warn(msg)
      db.runTransaction(JobRepo.completionJobLog(logId, OffsetDateTime.now(), JobStatus.JOB_FAILURE, msg))
    }
  }

}
