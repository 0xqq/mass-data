package mass.job.repository

import java.time.OffsetDateTime

import mass.model.CommonStatus
import mass.model.job._
import mass.slick.SlickProfile.api._

class JobTriggerRowTable(tag: Tag) extends Table[JobTriggerRow](tag, "job_trigger") {
  def key = column[String]("key", O.PrimaryKey)
  def config = column[JobTrigger]("config")
  def creator = column[String]("creator")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (key, config.?, creator, createdAt) <> ((JobTriggerRow.apply _).tupled, JobTriggerRow.unapply)
}

class JobItemRowTable(tag: Tag) extends Table[JobItemRow](tag, "job_item") {
  def key = column[String]("key", O.PrimaryKey)
  def config = column[JobItem]("config")
  def creator = column[String]("creator")
  def scheduleCount = column[Long]("schedule_count")
  def status = column[JobStatus]("status")
  def lastScheduledAt = column[Option[OffsetDateTime]]("last_scheduled_at")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (key, config.?, creator, scheduleCount, status, lastScheduledAt, createdAt) <> ((JobItemRow.apply _).tupled, JobItemRow.unapply)
}

class JobScheduleRowTable(tag: Tag) extends Table[JobScheduleRow](tag, "job_schedule") {
  def id = column[String]("id")
  def jobKey = column[String]("job_key")
  def triggerKey = column[String]("trigger_key")
  def description = column[String]("description")
  def status = column[CommonStatus]("status")
  def createdAt = column[OffsetDateTime]("created_at")
  def _pk = primaryKey(tableName + "_pk", (jobKey, triggerKey))

  def * =
    (id, jobKey, triggerKey, description, status, createdAt) <> ((JobScheduleRow.apply _).tupled, JobScheduleRow.unapply)
}

class JobLogTable(tag: Tag) extends Table[JobLog](tag, "job_log") {
  def id = column[String]("id", O.PrimaryKey, O.SqlTypeObjectId)
  def scheduleId = column[String]("schedule_id")
  def jobKey = column[String]("job_key")
  def triggerKey = column[String]("trigger_key")
  def startTime = column[OffsetDateTime]("start_time")
  def completionTime = column[Option[OffsetDateTime]]("completion_time")
  def completionStatus = column[JobStatus]("completion_status")
  def completionValue = column[Option[String]]("completion_value", O.SqlType("text"))
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, scheduleId, jobKey, triggerKey, startTime, completionTime, completionStatus, completionValue, createdAt) <> ((JobLog.apply _).tupled, JobLog.unapply)
}
