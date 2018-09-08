package mass.core.job

import mass.model.job.{JobItem, JobTrigger}

case class SchedulerContext(
    detailConfig: JobItem,
    triggerConf: JobTrigger,
    data: Map[String, String],
    resources: Map[String, String],
    scheduler: SchedulerSystemRef)
