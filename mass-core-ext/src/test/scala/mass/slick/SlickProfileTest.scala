package mass.slick

import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import SlickProfile.api._
import helloscala.common.Configuration
import mass.core.jdbc.JdbcUtils

class SlickProfileTest extends WordSpec with BeforeAndAfterAll {
  val configuration = Configuration()
  private val postgresProps = configuration.getConfiguration("mass.core.persistence.postgres")
  val ds = JdbcUtils.createHikariDataSource(postgresProps)
  val db = createDatabase(ds, postgresProps)

  "test" in {
    TimeUnit.SECONDS.sleep(1)
  }

  override protected def beforeAll(): Unit = super.beforeAll()
  override protected def afterAll(): Unit =
    db.close()
}
