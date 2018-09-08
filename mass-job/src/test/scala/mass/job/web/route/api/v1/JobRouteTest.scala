package mass.job.web.route.api.v1

import akka.http.scaladsl.model.StatusCodes
import mass.job.SchedulerSpec
import mass.job.business.Services
import mass.job.business.actors.JobActor
import mass.job.model.JobPageResp

class JobRouteTest extends SchedulerSpec {

  private lazy val services = new Services(
    jobSystem,
    List(
      JobActor.props(jobSystem)
    )
  )
  private lazy val route = new JobRoute(services).route

  "JobRoute" should {
    "page" in {
      import mass.http.JacksonSupport._
      Get("/job/page") ~> route ~> check {
        status mustBe StatusCodes.OK
        val resp = responseAs[JobPageResp]
        println(resp)
        resp must not be null
      }
    }

  }

}
