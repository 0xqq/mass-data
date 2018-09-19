package mass.job.business.components

import helloscala.common.Configuration
import helloscala.common.jackson.Jackson
import helloscala.common.test.HelloscalaSpec
import mass.job.JobSettings
import mass.model.job.{JobItem, Program}

class JobRunTest extends HelloscalaSpec {
  val configuration = Configuration()

  "JobRunTest" should {
    "run java" in {
      val detail = JobItem("test-key", Program.JAVA, Seq(), "test.JavaMain")
      val schedulerConfig = JobSettings(configuration)
      val result = JobRun.run(detail, "triggerKey", schedulerConfig)
      println(Jackson.prettyStringify(result))
      result.exitValue mustBe 0
      result.start must be < result.end
    }

    "run scala" in {
      val detail = JobItem("test-key", Program.SCALA, Seq(), "test.ScalaMain")
      val schedulerConfig = JobSettings(configuration)
      val result = JobRun.run(detail, "triggerKey", schedulerConfig)
      println(Jackson.prettyStringify(result))
      result.exitValue mustBe 0
      result.start must be < result.end
    }

    "run bash -c" in {
      val detail = JobItem("test-key", Program.SH, Seq("-c"), "echo '哈哈哈'")
      val schedulerConfig = JobSettings(configuration)
      val result = JobRun.run(detail, "triggerKey", schedulerConfig)
      println(Jackson.prettyStringify(result))
      result.exitValue mustBe 0
      result.start must be < result.end
    }

    "run python -c" in {
      val detail = JobItem("test-key", Program.PYTHON, Seq("-c"), "print('哈哈哈')")
      val schedulerConfig = JobSettings(configuration)
      val result = JobRun.run(detail, "triggerKey", schedulerConfig)
      println(Jackson.prettyStringify(result))
      result.exitValue mustBe 0
      result.start must be < result.end
    }

  }

}
