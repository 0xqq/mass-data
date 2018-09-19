package mass.core.json

import java.time.{Instant, OffsetDateTime}

import com.fasterxml.jackson.databind.node.IntNode
import helloscala.common.jackson.Jackson
import helloscala.common.test.HelloscalaSpec
import helloscala.common.util.TimeUtils
import mass.core.ScalaPBProtos
import mass.model.job.{JobItem, TriggerType}
import scalapb.{GeneratedEnum, GeneratedEnumCompanion}

class MassCoreJacksonModuleTest extends HelloscalaSpec {

  "ScalaPBJacksonModule" should {
    "test" in {
      val clz1 = TriggerType.CRON.getClass
      val clz1Name = clz1.getName

      println(clz1.getName)
      println(clz1.getName.take(clz1Name.indexOf('$')))
//      val clz2 = classOf[TriggerType.CRON.type]
      val clz3 = classOf[GeneratedEnumCompanion[GeneratedEnum]]
      println(clz1)
//      println(clz2)
      println(clz3)
    }

    "timestamp" in {
      val item = JobItem(createdAt = OffsetDateTime.now())
      println(item.createdAt)
      println(Jackson.prettyStringify(item))
      println(item.createdAt.toEpochSecond + "." + item.createdAt.getNano)
      println(Instant.ofEpochSecond(item.createdAt.toEpochSecond, item.createdAt.getNano))
      println(
        Instant
          .ofEpochSecond(item.createdAt.toEpochSecond, item.createdAt.getNano)
          .atOffset(TimeUtils.ZONE_CHINA_OFFSET))

      println(Jackson.valueToTree(new java.util.Date()))
    }

    "enumCompanions" in {
      println(ScalaPBProtos.messagesCompanions.size)
      ScalaPBProtos.enumerationMessagesCompanions.foreach(println)
      println(TriggerType.isInstanceOf[GeneratedEnumCompanion[_]])
    }

    "enum" in {
      println(TriggerType.getClass.toString)
      val companionName = TriggerType.getClass.getName
      println(companionName.take(companionName.indexOf('$')))

      val node = new IntNode(1)
      println(node)
      println(Jackson.treeToValue[TriggerType](node))
    }
  }

}
