package mass.slick

import helloscala.common.Configuration
import javax.sql.DataSource
import mass.core.jdbc.{JdbcTemplate, JdbcUtils}
import slick.basic.DatabasePublisher

import scala.concurrent.{ExecutionContext, Future}

class SqlManager private (conf: Configuration) {
  import SlickProfile.api._

  val profile: SlickProfile.type = SlickProfile
  val dataSource: DataSource = JdbcUtils.createHikariDataSource(conf)
  val slickDatabase: SlickProfile.backend.DatabaseDef = createDatabase(dataSource, conf)
  val jdbcTemplate: JdbcTemplate = JdbcTemplate(dataSource, conf)

  implicit final def executionContext: ExecutionContext = slickDatabase.ioExecutionContext

  def runTransaction[R, E <: Effect.Write](a: DBIOAction[R, NoStream, E]): Future[R] =
    slickDatabase.run(a.transactionally)

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = slickDatabase.run(a)

  def stream[T](a: DBIOAction[_, Streaming[T], Nothing]): DatabasePublisher[T] = slickDatabase.stream(a)

  def streamTransaction[T, E <: Effect.Write](a: DBIOAction[_, Streaming[T], E]): DatabasePublisher[T] =
    slickDatabase.stream(a.transactionally)

  override def toString = s"${getClass.getSimpleName}($dataSource, $jdbcTemplate, $slickDatabase)"
}

object SqlManager {
  val DEFAULT_PATH = "mass.core.persistence.postgres"

  def apply(configuration: Configuration, path: String = DEFAULT_PATH): SqlManager =
    new SqlManager(configuration.getConfiguration(path))
}
