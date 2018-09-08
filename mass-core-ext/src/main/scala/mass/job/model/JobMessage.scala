package mass.job.model

import java.io.File
import java.nio.charset.Charset

import helloscala.common.page.{Page, PageResult}
import mass.message.job.JobMessage
import mass.model.job.JobTrigger

// TODO 迁移到 proto 中定义
case class JobPageResp(
    content: Seq[JobItemRow] = Nil,
    totalElements: Long = 0,
    triggers: Seq[JobTrigger] = Nil,
    page: Int = Page.DEFAULT_PAGE,
    size: Int = Page.DEFAULT_SIZE
) extends PageResult[JobItemRow]
    with JobMessage

case class JobUploadJobReq(file: File, fileName: String, charset: Charset) extends JobMessage

case class JobUpdateTriggerResp(item: JobTrigger)
