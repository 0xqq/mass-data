package mass.job.model

import java.io.File
import java.nio.charset.Charset

import mass.message.job.JobMessage

// 不需要在节点间传输，不用在 protobuf 里定义
case class JobUploadJobReq(file: File, fileName: String, charset: Charset) extends JobMessage
