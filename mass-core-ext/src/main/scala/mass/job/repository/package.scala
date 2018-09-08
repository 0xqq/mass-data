package mass.job

import mass.slick.SlickProfile.api._

package object repository {

  def tJobItem: TableQuery[JobItemTable] = TableQuery[JobItemTable]

  def tJobTrigger: TableQuery[JobTriggerTable] = TableQuery[JobTriggerTable]

  def tJobSchedule: TableQuery[JobScheduleTable] = TableQuery[JobScheduleTable]

  def tJobLog: TableQuery[JobLogTable] = TableQuery[JobLogTable]

}
