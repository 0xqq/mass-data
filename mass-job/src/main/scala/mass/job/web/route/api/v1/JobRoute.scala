package mass.job.web.route.api.v1

import java.nio.file.Files

import akka.http.scaladsl.server.Route
import akka.pattern._
import com.typesafe.scalalogging.StrictLogging
import helloscala.common.page.Page
import mass.http.AbstractRoute
import mass.message.job._
import mass.job.business.Services
import mass.job.business.actors.JobActor
import mass.job.model.JobUploadJobReq

class JobRoute(services: Services) extends AbstractRoute with StrictLogging {
  import services._

  override def route: Route = pathPrefix("job") {
    pageRoute ~
      itemByKeyRoute ~
      triggerByKeyRoute ~
      triggerPutRoute ~
      uploadJobPostRoute
  }

  def itemByKeyRoute: Route = pathGet("item" / Segment) { key =>
    futureComplete((master ? JobActor.jobMessage(JobListJobItemReq(jobKey = key))).mapTo[JobListJobItemResp])
  }

  def triggerByKeyRoute: Route = pathGet("trigger" / Segment) { key =>
    futureComplete((master ? JobActor.jobMessage(JobListJobTriggerReq(key = key))).mapTo[JobListJobTriggerResp])
  }

  val pagePDM =
    ('page.as[Int].?(Page.DEFAULT_PAGE), 'size.as[Int].?(Page.DEFAULT_SIZE), 'jobKey.?(""), 'triggerKey.?(""))

  def pageRoute: Route = pathGet("page") {
    parameters(pagePDM).as(JobPageReq.apply) { req =>
      futureComplete(master ? JobActor.jobMessage(req))
    }
  }

  def triggerPutRoute: Route = pathPut("trigger") {
    import mass.http.JacksonSupport._
    entity(as[JobUpdateTriggerReq]) { req =>
      futureComplete(master ? JobActor.jobMessage(req))
    }
  }

  def uploadJobPostRoute: Route = pathPost("upload_job") {
    extractExecutionContext { implicit ec =>
      storeUploadedFile("job", createTempFileFunc(services.jobSystem.massSystem.tempDir)) {
        case (fileInfo, file) =>
          val future =
            (master ? JobActor.jobMessage(JobUploadJobReq(file, fileInfo.fileName, fileInfo.contentType.charset)))
              .andThen { case _ => Files.deleteIfExists(file.toPath) }
          onSuccess(future) { result =>
            objectComplete(result)
          }
      }
    }
  }

}