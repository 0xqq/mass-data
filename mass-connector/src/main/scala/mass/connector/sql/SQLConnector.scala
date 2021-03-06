package mass.connector.sql

import com.zaxxer.hikari.HikariDataSource
import mass.connector.ConnectorType.ConnectorType
import mass.connector.{Connector, ConnectorSetting, ConnectorType}
import mass.core.jdbc.{JdbcTemplate, JdbcUtils}

/**
  *
  */
final case class SQLConnector(name: String, setting: ConnectorSetting)
    extends Connector {

  override def `type`: ConnectorType = ConnectorType.JDBC

  lazy val dataSource: HikariDataSource =
    JdbcUtils.createHikariDataSource(configuration)
  lazy val jdbcTemplate = JdbcTemplate(
    dataSource,
    configuration.getOrElse("use-transaction", true),
    configuration.getOrElse("ignore-warnings", true),
    configuration.getOrElse("allow-print-log", false)
  )

  override def close(): Unit = dataSource.close()
}
