package mass.job.model

import java.time.OffsetDateTime

import mass.model.job.JobItem

case class JobItemRow(key: String, config: JobItem, creator: String, createdAt: OffsetDateTime)

object JobItems {

  object Resources {
    val ZIP_PATH = "ZIP_PATH"
  }

}
