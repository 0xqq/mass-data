package mass.job.model

import java.time.OffsetDateTime

import helloscala.common.types.ObjectId
import mass.model.CommonStatus

case class JobLog(
    id: ObjectId,
    jobKey: String,
    triggerKey: String,
    startTime: OffsetDateTime,
    completionTime: Option[OffsetDateTime],
    completionStatus: CommonStatus,
    completionValue: Option[String],
    createdAt: OffsetDateTime)
