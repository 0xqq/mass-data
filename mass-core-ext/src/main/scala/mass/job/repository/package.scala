package mass.job

import mass.slick.SlickProfile.api._

package object repository {

  def tJobItem: TableQuery[JobItemRowTable] = TableQuery[JobItemRowTable]

  def tJobTrigger: TableQuery[JobTriggerRowTable] = TableQuery[JobTriggerRowTable]

  def tJobSchedule: TableQuery[JobScheduleRowTable] = TableQuery[JobScheduleRowTable]

  def tJobLog: TableQuery[JobLogTable] = TableQuery[JobLogTable]
}
