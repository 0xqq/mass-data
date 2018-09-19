package mass.job

import java.nio.file.Files
import java.time.OffsetDateTime
import java.util.Properties

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import helloscala.common.Configuration
import helloscala.common.exception.HSBadRequestException
import mass.core.job.{JobConstants, SchedulerJob, SchedulerSystemRef}
import mass.model.job.{JobItem, JobTrigger, TriggerType}
import mass.server.MassSystemExtension

import scala.collection.mutable
import scala.concurrent.ExecutionContext

object JobSystem {
  private var _instance: JobSystem = _

  def instance: JobSystem = _instance

  def apply(massSystem: MassSystemExtension): JobSystem =
    apply(massSystem.name, massSystem, true)

  def apply(name: String, massSystem: MassSystemExtension, waitForJobsToComplete: Boolean): JobSystem = {
    _instance = new JobSystem(name, massSystem, waitForJobsToComplete)
    _instance.init()
    _instance
  }

}

class JobSystem private (
    val name: String,
    val massSystem: MassSystemExtension,
    val waitForJobsToComplete: Boolean
) extends SchedulerSystemRef
    with LazyLogging {
  import org.quartz._
  import org.quartz.impl.StdSchedulerFactory

  private val scheduler: org.quartz.Scheduler =
    new StdSchedulerFactory(configuration.get[Properties]("mass.core.job.properties")).getScheduler
  val jobSettings = JobSettings(configuration)

  // 事件触发待执行Job队列。
  // 当事件发生时，执行任务
  private val eventTriggerJobs = mutable.Map[String, List[(JobItem, JobTrigger)]]()

  def init(): Unit = {
    if (!Files.isDirectory(jobSettings.jobSavedDir)) {
      Files.createDirectories(jobSettings.jobSavedDir)
    }

    scheduler.start()
    massSystem.system.registerOnTermination {
      scheduler.shutdown(waitForJobsToComplete)
    }
  }

  // TODO 定义 SchedulerSystem 自有的线程执行器
  implicit override def executionContext: ExecutionContext = massSystem.system.dispatcher

  override def system: ActorSystem = massSystem.system

  override def configuration: Configuration = massSystem.configuration

  def rescheduleJob(conf: JobTrigger, jobItems: Seq[JobItem], className: String): OffsetDateTime = {
    import scala.collection.JavaConverters._
    scheduler.deleteJobs(jobItems.map(item => JobKey.jobKey(item.key)).asJava)
    for (item <- jobItems) {
      val trigger = buildTrigger(conf, jobKey = Some(item.key))
      scheduler.scheduleJob(buildJobDetail(item, className, Some(item.data)), trigger)
    }
    OffsetDateTime.now()
  }

  def schedulerJob(
      jobItem: JobItem,
      jobTrigger: JobTrigger,
      className: String,
      data: Option[Map[String, String]],
      replace: Boolean = true): OffsetDateTime =
    jobTrigger.triggerType match {
      case TriggerType.EVENT =>
        handleTriggerEventJob(jobItem, jobTrigger)
      case _ =>
        val jobDetail = Option(scheduler.getJobDetail(JobKey.jobKey(jobItem.key))) getOrElse
          buildJobDetail(jobItem, className, data)
        val trigger = Option(scheduler.getTrigger(TriggerKey.triggerKey(jobTrigger.key))) getOrElse
          buildTrigger(jobTrigger)
        schedulerJob(jobDetail, trigger, replace)
    }

  def schedulerJob(jobDetail: JobDetail, trigger: Trigger, replace: Boolean): OffsetDateTime = {
    scheduler.scheduleJob(jobDetail, java.util.Collections.singleton(trigger), replace)
    logger.info(s"启动作业：${jobDetail.getKey}:${trigger.getKey}, $replace")
    OffsetDateTime.now()
  }

  private def handleTriggerEventJob(detailConfig: JobItem, triggerConf: JobTrigger): OffsetDateTime = {
    // 将事件触发Job加入队列
    val value = (detailConfig, triggerConf)
    val values = eventTriggerJobs.get(triggerConf.triggerEvent) match {
      case Some(list) => value :: list.filterNot { case (_, exist) => exist.key == triggerConf.key }
      case _          => value :: Nil
    }
    eventTriggerJobs.put(triggerConf.triggerEvent, values)
    OffsetDateTime.now()
  }

  private def buildTrigger(conf: JobTrigger, jobKey: Option[String] = None): Trigger = {
    var builder: TriggerBuilder[Trigger] =
      TriggerBuilder.newTrigger().withIdentity(TriggerKey.triggerKey(conf.key))

    conf.startTime.foreach(st => builder = builder.startAt(java.util.Date.from(st.toInstant)))
    conf.endTime.foreach(et => builder = builder.endAt(java.util.Date.from(et.toInstant)))
    jobKey.foreach(key => builder = builder.forJob(key))

    val schedule = conf.triggerType match {
      case TriggerType.SIMPLE =>
        val ssb = SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(conf.duration.toMillis)
        if (conf.repeat > 0) ssb.withRepeatCount(conf.repeat) else ssb.repeatForever()
      case TriggerType.CRON => CronScheduleBuilder.cronSchedule(conf.cronExpress)
      case other            => throw HSBadRequestException(s"无效的触发器类型：$other")
    }
    builder.withSchedule(schedule).build()
  }

  private def buildJobDetail(
      config: JobItem,
      className: String,
      data: Option[Map[String, String]]
  ): JobDetail = {
    require(classOf[SchedulerJob].isAssignableFrom(Class.forName(className)),
            s"className 必需为 ${classOf[SchedulerJob].getName} 的子类")
    val dataMap = new JobDataMap()
    dataMap.put(JobConstants.JOB_CLASS, className)
    for ((key, value) <- data.getOrElse(config.data)) {
      dataMap.put(key, value)
    }
    JobBuilder
      .newJob(classOf[JobClassJob])
      .withIdentity(JobKey.jobKey(config.key))
      .setJobData(dataMap)
      .build()
  }

  override def toString: String =
    s"SchedulerSystem($name, $massSystem, $waitForJobsToComplete)"
}
