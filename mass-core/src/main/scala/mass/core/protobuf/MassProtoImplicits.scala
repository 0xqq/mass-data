package mass.core.protobuf

import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

import helloscala.common.util.{StringUtils, TimeUtils}
import scalapb.TypeMapper

import scala.concurrent.duration.FiniteDuration

object MassProtoImplicits {
  implicit val int64ToOffsetDateTimeMapper: TypeMapper[Long, OffsetDateTime] =
    TypeMapper[Long, OffsetDateTime](millis => TimeUtils.toOffsetDateTime(millis))(_.toInstant.toEpochMilli)

  implicit val int64ToOffsetDateTimeOptionMapper: TypeMapper[Long, Option[OffsetDateTime]] =
    TypeMapper[Long, Option[OffsetDateTime]](millis =>
      if (millis == 0L) None else Some(TimeUtils.toOffsetDateTime(millis)))(
      _.map(_.toInstant.toEpochMilli).getOrElse(0L))

  implicit val stringToOffsetDateTimeMapper: TypeMapper[String, OffsetDateTime] =
    TypeMapper[String, OffsetDateTime](TimeUtils.toOffsetDateTime)(_.toString)

  implicit val stringToOffsetDateTimeOptionMapper: TypeMapper[String, Option[OffsetDateTime]] =
    TypeMapper[String, Option[OffsetDateTime]](str => StringUtils.option(str).map(TimeUtils.toOffsetDateTime))(
      _.map(_.toString).getOrElse(""))

  implicit val int64ToFiniteDurationMapper: TypeMapper[Long, FiniteDuration] =
    TypeMapper[Long, FiniteDuration](l => FiniteDuration(l, TimeUnit.MILLISECONDS))(_.toMillis)

  implicit val int64ToFiniteDurationOptionMapper: TypeMapper[Long, Option[FiniteDuration]] =
    TypeMapper[Long, Option[FiniteDuration]](l =>
      if (l == 0L) None else Some(FiniteDuration(l, TimeUnit.MILLISECONDS)))(_.map(_.toMillis).getOrElse(0L))
}