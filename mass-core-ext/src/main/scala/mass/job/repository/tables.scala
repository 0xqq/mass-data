package mass.job.repository

import java.time.OffsetDateTime

import helloscala.common.types.ObjectId
import mass.job.model.{JobItemRow, JobLog, JobTriggerRow}
import mass.model.CommonStatus
import mass.model.job._
import mass.slick.SlickProfile.api._

class JobTriggerTable(tag: Tag) extends Table[JobTriggerRow](tag, "job_trigger") {
  def key = column[String]("key", O.PrimaryKey)
  def config = column[JobTrigger]("config")
  def creator = column[String]("creator")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (key, config, creator, createdAt).mapTo[JobTriggerRow]
}

class JobItemTable(tag: Tag) extends Table[JobItemRow](tag, "job_item") {
  def key = column[String]("key", O.PrimaryKey)
  def config = column[JobItem]("config")
  def creator = column[String]("creator")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (key, config, creator, createdAt).mapTo[JobItemRow]
}

class JobScheduleTable(tag: Tag) extends Table[JobScheduleRow](tag, "job_schedule") {
  def jobKey = column[String]("job_key")
  def triggerKey = column[String]("trigger_key")
  def description = column[String]("description")
  def status = column[CommonStatus]("status")
  def createdAt = column[OffsetDateTime]("created_at")
  def _pk = primaryKey(tableName + "_pk", (jobKey, triggerKey))

  def * =
    (jobKey, triggerKey, description, status, createdAt) <> ((JobScheduleRow.apply _).tupled, JobScheduleRow.unapply)
}

class JobLogTable(tag: Tag) extends Table[JobLog](tag, "job_log") {
  def id = column[ObjectId]("id", O.PrimaryKey, O.SqlTypeObjectId)
  def jobKey = column[String]("job_key")
  def triggerKey = column[String]("trigger_key")
  def startTime = column[OffsetDateTime]("start_time")
  def completionTime = column[Option[OffsetDateTime]]("completion_time")
  def completionStatus = column[CommonStatus]("completion_status")
  def completionValue = column[Option[String]]("completion_value", O.SqlType("text"))
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, jobKey, triggerKey, startTime, completionTime, completionStatus, completionValue, createdAt).mapTo[JobLog]
}
