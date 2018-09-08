package mass.job.repository

import java.time.OffsetDateTime

import helloscala.common.exception.HSBadRequestException
import helloscala.common.types.ObjectId
import helloscala.common.util.StringUtils
import mass.job.model._
import mass.job.util.JobZip
import mass.message.job.{JobListJobItemReq, JobListJobTriggerReq, JobPageReq}
import mass.model.CommonStatus
import mass.model.job.{JobScheduleRow, JobTrigger}
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
      jobZip: JobZip,
      creator: String = ""
  )(implicit ec: ExecutionContext): DBIOAction[Vector[(JobItemRow, Option[JobTrigger])], NoStream, Effect.Write] = {
    val now = OffsetDateTime.now()
    val actions = jobZip.configs.map { config =>
      val jobItem = config.item.getOrElse(throw HSBadRequestException("detail不能为空"))
      val maybeSchedule = config.trigger.map(trigger =>
        JobScheduleRow(jobItem.key, trigger.key, s"triggerType: ${trigger.triggerType}", CommonStatus.ENABLE, now))
      for {
        result <- tJobItem returning tJobItem += JobItemRow(jobItem.key, jobItem, creator, now)
        _ <- config.trigger
              .map(trigger => tJobTrigger += JobTriggerRow(trigger.key, trigger, creator, now))
              .getOrElse(DBIO.successful(()))
        _ <- maybeSchedule.map(tJobSchedule += _).getOrElse(DBIO.successful(()))
      } yield result -> config.trigger
    }
    DBIO.sequence(actions)
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
      triggers <- tJobTrigger
                   .join(tJobSchedule)
                   .on((t, s) => t.key === s.triggerKey && s.jobKey.inSet(content.map(_.key)))
                   .map(_._1.config)
                   .result
    } yield JobPageResp(content, total, triggers, req.page, req.size)
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

  def insertJobLog(jobLog: JobLog): FixedSqlAction[Int, NoStream, Effect.Write] =
    tJobLog += jobLog

  def completionJobLog(
      id: ObjectId,
      completionTime: OffsetDateTime,
      completionStatus: CommonStatus,
      completionValue: String
  ): FixedSqlAction[Int, NoStream, Effect.Write] =
    tJobLog
      .filter(_.id === id)
      .map(r => (r.completionTime, r.completionStatus, r.completionValue))
      .update((Some(completionTime), completionStatus, Some(completionValue)))

}
