package helloscala.data
import com.typesafe.scalalogging.Logger
import helloscala.common.jackson.Jackson

import scala.util.control.NonFatal

object Provinces {
  private val logger = Logger(getClass.getName.dropRight(1))

  lazy val provinces: ProvinceData = getProvinces()

//
  def getProvinces(): ProvinceData = {
    val in =
      Thread.currentThread().getContextClassLoader.getResource("province.json")
    try {
      Jackson.defaultObjectMapper.readValue(in, classOf[ProvinceData])
    } catch {
      case NonFatal(e) =>
        logger.error("获取省份名名称数据错误", e)
        throw e
    }
  }

}
