package mass.job.repository

import java.time.OffsetDateTime

import helloscala.common.exception.HSBadRequestException
import helloscala.common.util.StringUtils
import mass.job.util.JobZip
import mass.message.job.{JobListJobItemReq, JobListJobTriggerReq, JobPageReq, JobPageResp}
import mass.model.{CommonStatus, KeyValues}
import mass.model.job._
import mass.slick.SlickProfile.api._
import slick.sql.{FixedSqlAction, FixedSqlStreamingAction, SqlAction}

import scala.concurrent.ExecutionContext

object JobRepo {

  def listSchedule(
      jobKey: Option[String] = None,
      triggerKey: Option[String] = None
  ): FixedSqlStreamingAction[Seq[JobScheduleRow], JobScheduleRow, Effect.Read] =
    tJobSchedule
      .filter(t => dynamicFilter(jobKey.map(t.jobKey === _), triggerKey.map(t.triggerKey === _)))
      .sortBy(t => t.createdAt.desc)
      .result

  def listJobItem(req: JobListJobItemReq): FixedSqlStreamingAction[Seq[JobItemRow], JobItemRow, Effect.Read] =
    tJobItem
      .joinLeft(tJobSchedule)
      .on(
        (i, t) =>
          dynamicFilter(Some(i.key === t.jobKey),
                        StringUtils.option(req.jobKey).map(t.jobKey === _),
                        StringUtils.option(req.triggerKey).map(t.triggerKey === _)))
      .sortBy(_._1.createdAt.desc)
      .map(_._1)
      .result

  def save(
      item: JobItem,
      trigger: Option[JobTrigger],
      description: String): DBIOAction[AnyVal, NoStream, Effect.Write] = {
    val now = OffsetDateTime.now
    val a = tJobItem.insertOrUpdate(JobItemRow(item.key, Some(item), createdAt = now))
    trigger match {
      case Some(tg) =>
        DBIO.seq(
          a,
          tJobTrigger.insertOrUpdate(JobTriggerRow(tg.key, trigger, createdAt = now)),
          tJobSchedule.insertOrUpdate(
            JobScheduleRow(item.key + tg.key, item.key, tg.key, description, CommonStatus.Continue, now))
        )
      case _ => a
    }
  }

  def save(
      jobZip: JobZip,
      creator: String = ""
  )(implicit ec: ExecutionContext): DBIOAction[Vector[(JobItemRow, Option[JobTrigger])], NoStream, Effect.Write] = {
    val now = OffsetDateTime.now()
    val actions = jobZip.configs.map { config =>
      val jobItem = config.item.getOrElse(throw HSBadRequestException("detail不能为空"))
      val maybeSchedule = config.trigger.map(
        trigger =>
          JobScheduleRow(jobItem.key + trigger.key,
                         jobItem.key,
                         trigger.key,
                         s"triggerType: ${trigger.triggerType}",
                         CommonStatus.ENABLE,
                         now))
      for {
        result <- tJobItem returning tJobItem += JobItemRow(jobItem.key, Some(jobItem), creator, createdAt = now)
        _ <- config.trigger
              .map(trigger => tJobTrigger += JobTriggerRow(trigger.key, Some(trigger), creator, now))
              .getOrElse(DBIO.successful(()))
        _ <- maybeSchedule.map(tJobSchedule += _).getOrElse(DBIO.successful(()))
      } yield result -> config.trigger
    }
    DBIO.sequence(actions)
  }

  /**
   * 获得JobItem.key 对应JobTrigger.key的列表
   * @param jobKeys JobItem.key seq
   * @return Seq[(JobItem.key, Seq[JobTrigger.key])]
   */
  def queryJobItemKeyTriggerKeys(
      jobKeys: Seq[String]
  ): Query[(Rep[String], Rep[Seq[String]]), (String, Seq[String]), Seq] = {
    import mass.slick.AggFuncSupport.GeneralAggFunctions._
    tJobItem
      .join(tJobSchedule)
      .on((item, schedule) => item.key === schedule.jobKey && item.key.inSet(jobKeys))
      .groupBy(_._1.key)
      .map {
        case (jobKey, css) =>
          val tk = css.map { case (_, schedule) => schedule.triggerKey }
          (jobKey, arrayAggEx(tk))
      }
  }

  def page(req: JobPageReq)(implicit ec: ExecutionContext): DBIOAction[JobPageResp, NoStream, Effect.Read] = {
    val q = tJobItem.filter(
      t =>
        dynamicFilter(
          StringUtils.option(req.jobKey).map(jobKey => t.key like s"%$jobKey%"),
          StringUtils
            .option(req.triggerKey)
            .map(triggerKey => t.key in tJobSchedule.filter(_.triggerKey like s"%$triggerKey%").map(_.jobKey))
      ))

    for {
      content <- q.sortBy(_.createdAt.desc).drop(req.offset).take(req.size).result
      total <- q.length.result
      jobKey2TriggerKeys <- queryJobItemKeyTriggerKeys(content.map(_.key)).result
      triggers <- tJobTrigger.filter(_.key inSet jobKey2TriggerKeys.flatMap(_._2)).map(_.config).result
    } yield {
      val keyValues = jobKey2TriggerKeys.map { case (key, values) => KeyValues(key, values) }
      JobPageResp(content, total, keyValues, triggers, req.page, req.size)
    }
  }

  def saveJobDetail(jobDetail: JobItemRow): FixedSqlAction[Option[JobItemRow], NoStream, Effect.Write] =
    tJobItem returning tJobItem insertOrUpdate jobDetail

  def findJob(
      jobKey: String,
      triggerKey: String
  )(implicit ec: ExecutionContext)
    : DBIOAction[Option[(JobItemRow, JobTriggerRow)], NoStream, Effect.Read with Effect.Read] =
    for {
      jobItem <- findJobItem(jobKey)
      jobTrigger <- findJobTrigger(triggerKey)
    } yield jobItem.flatMap(item => jobTrigger.map(item -> _))

  def findJobItem(
      key: String
  ): SqlAction[Option[JobItemRow], NoStream, Effect.Read] =
    tJobItem.filter(_.key === key).result.headOption

  def saveJobTrigger(jobTrigger: JobTriggerRow): FixedSqlAction[Option[JobTriggerRow], NoStream, Effect.Write] =
    tJobTrigger returning tJobTrigger insertOrUpdate jobTrigger

  def findJobTrigger(
      key: String
  ): SqlAction[Option[JobTriggerRow], NoStream, Effect.Read] =
    tJobTrigger.filter(_.key === key).result.headOption

  def listJobTrigger(
      req: JobListJobTriggerReq): FixedSqlStreamingAction[Seq[JobTriggerRow], JobTriggerRow, Effect.Read] =
    tJobTrigger.filter(t => t.key === req.key).sortBy(_.createdAt.desc).result

  def insertJobSchedule(jobSchedule: JobScheduleRow): FixedSqlAction[JobScheduleRow, NoStream, Effect.Write] =
    tJobSchedule returning tJobSchedule += jobSchedule

  def updateJobSchedule(
      jobKey: String,
      triggerKey: String,
      status: CommonStatus): FixedSqlAction[Int, NoStream, Effect.Write] =
    tJobSchedule.filter(t => t.jobKey === jobKey && t.triggerKey === triggerKey).map(_.status).update(status)

  def updateJobItemScheduledAt(jobKey: String, scheduledAt: OffsetDateTime): SqlAction[Int, NoStream, Effect] = {
    import mass.slick.SlickProfile.plainApi._
    sqlu"""update job_item set schedule_count = schedule_count + 1 where key = $jobKey and last_scheduled_at = $scheduledAt"""
  }

  def updateJobItemStatus(jobKey: String, status: JobStatus): FixedSqlAction[Int, NoStream, Effect.Write] = {
    tJobItem.filter(_.key === jobKey).map(_.status).update(status)
  }

  def updateJobItemStatusByLogId(logId: String, status: JobStatus): FixedSqlAction[Int, NoStream, Effect.Write] = {
    tJobLog
      .join(tJobItem)
      .on((log, item) => log.jobKey === item.key && log.id === logId)
      .map(_._2.status)
      .update(status)
  }

  def insertJobLog(jobLog: JobLog): DBIOAction[Unit, NoStream, Effect.Write] = {
    DBIO.seq(tJobLog += jobLog,
             updateJobItemScheduledAt(jobLog.jobKey, jobLog.startTime),
             updateJobItemStatus(jobLog.jobKey, jobLog.completionStatus))
  }

  def completionJobLog(
      id: String,
      completionTime: OffsetDateTime,
      completionStatus: JobStatus,
      completionValue: String
  ): DBIOAction[Unit, NoStream, Effect.Write] = {
    DBIO.seq(
      tJobLog
        .filter(_.id === id)
        .map(r => (r.completionTime, r.completionStatus, r.completionValue))
        .update((Some(completionTime), completionStatus, Some(completionValue))),
      updateJobItemStatusByLogId(id, completionStatus)
    )
  }

}
