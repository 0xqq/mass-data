package mass.job.boot

import mass.core.MassSystem
import mass.job.JobSystem
import mass.server.MassSystemExtension

object JobMain extends App {
  val massSystem = MassSystem().as[MassSystemExtension]
  val jobSystem = JobSystem(massSystem)
  new JobServer(jobSystem).startServerAwait()
}
