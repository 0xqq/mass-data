package mass.server.repository

import java.time.{LocalDateTime, OffsetDateTime}

import akka.actor.ActorSystem
import akka.testkit.TestKit
import helloscala.common.jackson.Jackson
import helloscala.common.test.HelloscalaSpec
import helloscala.common.util.TimeUtils
import mass.extension.MassExSystem
import mass.job.repository._
import mass.slick.SlickProfile.api._
import org.scalatest.BeforeAndAfterAll

class JobRepoTest extends TestKit(ActorSystem("test")) with HelloscalaSpec with BeforeAndAfterAll {

  private val massSystem: MassExSystem = MassExSystem(system)

  "JobRepositoryTest" should {
    val db = massSystem.sqlManager.slickDatabase

    "saveJobDetail" in {
//      val jobDetail = JobItem("key", Map("className" -> "java.lang.String"), None, OffsetDateTime.now())
//      val result = db.run(jobRepo.saveJobDetail(jobDetail)).futureValue
//      println(s"saveJobDetail: $result")
    }

    "saveJobTrigger" in {
//      val jobTrigger = JobTrigger("key", Some("10 * * * * ?"), None, None, None, None, None, OffsetDateTime.now())
//      val result = db.run(jobRepo.saveJobTrigger(jobTrigger)).futureValue
//      println(s"saveJobTrigger: $result")
    }

    "dd" in {
      import mass.slick.AggFuncSupport.GeneralAggFunctions._
      val q = tJobItem
        .join(tJobSchedule)
        .on((item, schedule) => item.key === schedule.jobKey)
        .groupBy(_._1.key)
        .map {
          case (jobKey, css) =>
            val tk = css.map { case (_, schedule) => schedule.triggerKey }

            (jobKey, arrayAggEx(tk))
        }
//        .groupBy(_._1)
      q.result.statements.foreach(println)
    }
  }

  "JSON" should {
    "trigger" in {
      val jstr = """{"key":"kettle","triggerType":1,"startTime":"2018-09-12T13:00:11.459Z","endTime":null}"""
      val jnode = Jackson.readTree(jstr)
      println(jnode)
//      val trigger = Jackson.treeToValue[JobTrigger](jnode)
//      println(trigger)
      val odt = Jackson.treeToValue[LocalDateTime](jnode.get("startTime"))
      println(odt.atOffset(TimeUtils.ZONE_CHINA_OFFSET))
      println(odt.toString)
      println(odt.format(TimeUtils.formatterDateTime))
      println(OffsetDateTime.of(2018, 9, 12, 13, 0, 11, 0, TimeUtils.ZONE_CHINA_OFFSET))
    }
  }

}
