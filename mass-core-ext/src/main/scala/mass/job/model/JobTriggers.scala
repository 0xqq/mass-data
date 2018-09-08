package mass.job.model

import java.time.OffsetDateTime

import mass.model.job.JobTrigger

case class JobTriggerRow(key: String, config: JobTrigger, creator: String, createdAt: OffsetDateTime)

object JobTriggers {}
