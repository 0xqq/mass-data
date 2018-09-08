/*
 * Copyright (c) Yangbajing 2018
 *
 * This is the custom License of Yangbajing
 */

package mass.server

import akka.actor.Address
import com.typesafe.scalalogging.StrictLogging
import helloscala.common.Configuration
import mass.core.Constants

case class MassConfig(configuration: Configuration) extends StrictLogging {
  if (!configuration.has("mass.core.scala212-home")) {
    logger.warn("未配置Scala 2.12主目录，config key: mass.core.scala212-home")
  }
  if (!configuration.has("mass.core.scala211-home")) {
    logger.warn("未配置Scala 2.11主目录，config key: mass.core.scala211-home")
  }

  def scala212Home: String = configuration.getOrElse[String]("mass.core.scala212-home", "")

  def scala211Home: String = configuration.getOrElse[String]("mass.core.scala211-home", "")

  def clusterName: String =
    configuration.getString(Constants.BASE_CONF + ".cluster.name")

  def clusterProtocol: String =
    configuration.getString(Constants.BASE_CONF + ".cluster.protocol")

  def clusterSeeds: List[Address] =
    configuration
      .get[Seq[String]](Constants.BASE_CONF + ".cluster.seeds")
      .map { seed =>
        val Array(host, port) = seed.split(':')
        Address(clusterProtocol, clusterName, host, port.toInt)
      }
      .toList

}
