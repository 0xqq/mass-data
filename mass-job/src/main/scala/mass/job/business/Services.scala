package mass.job.business

import akka.actor.{ActorRef, Props}
import akka.util.Timeout
import mass.job.JobSystem

import scala.concurrent.duration._

class Services(
    val jobSystem: JobSystem,
    propsList: Iterable[(Props, Symbol)]
) {
  implicit val timeout: Timeout = Timeout(10.seconds)

  val master: ActorRef =
    jobSystem.system
      .actorOf(SchedulerActor.props(jobSystem, propsList), SchedulerActor.name.name)
}
