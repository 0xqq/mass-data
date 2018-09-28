package mass.job.boot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import helloscala.common.Configuration
import mass.http.{AbstractRoute, HSAkkaHttpServer}
import mass.job.JobSystem
import mass.job.business.Services
import mass.job.business.actors.JobActor
import mass.job.web.route.Routes

import scala.concurrent.Future

class JobServer(jobSystem: JobSystem) extends HSAkkaHttpServer {

  override def actorSystem: ActorSystem = jobSystem.system

  override def actorMaterializer: ActorMaterializer =
    ActorMaterializer()(jobSystem.system)

  override val hlServerValue: String = "mass-scheduler"

  val services = new Services(jobSystem, List(JobActor.props(jobSystem)))

  override def configuration: Configuration = jobSystem.configuration

  override def routes: AbstractRoute = new Routes(services)

  override def close(): Unit = {}

  /**
   * 启动基于Akka HTTP的服务
   * @return
   */
  def startServer(): (Future[Http.ServerBinding], Option[Future[Http.ServerBinding]]) =
    startServer(
      configuration.getString("mass.job.server.host"),
      configuration.getInt("mass.job.server.port"),
      configuration.get[Option[Int]]("mass.job.server.https-port")
    )

}
