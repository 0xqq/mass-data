package mass.core.protobuf

import mass.core.enum.{EnumUtils, JobStatusEnum}
import mass.model.job.JobStatus
import mass.model.{IdValue, TitleIdValue}
import scalapb.{GeneratedEnum, GeneratedEnumCompanion, UnrecognizedEnum}

import scala.collection.mutable

object ProtoUtils {
  private val cacheIdValues = mutable.Map[GeneratedEnumCompanion[_ <: GeneratedEnum], Seq[IdValue]]()
  private val cacheTitleIdValues = mutable.Map[GeneratedEnumCompanion[_ <: GeneratedEnum], Seq[TitleIdValue]]()

  def enumToIdValues(list: Seq[GeneratedEnum]): Seq[IdValue] =
    list.filterNot(_.isInstanceOf[UnrecognizedEnum]).map(e => IdValue(e.value, e.name))

  def enumToTitleIdValues(list: Seq[GeneratedEnum]): Seq[TitleIdValue] =
    list.filterNot(_.isInstanceOf[UnrecognizedEnum]).map(e => TitleIdValue(e.name, e.value))

  def enumToIdValues(obj: GeneratedEnumCompanion[_ <: GeneratedEnum]): Seq[IdValue] = {
    cacheIdValues.getOrElseUpdate(obj, obj match {
      case _: GeneratedEnumCompanion[JobStatus] => EnumUtils.enumToIdValues(JobStatusEnum)
      case _                                    => enumToIdValues(obj.values)
    })
  }

  def enumToTitleIdValues(obj: GeneratedEnumCompanion[_ <: GeneratedEnum]): Seq[TitleIdValue] = {
    cacheTitleIdValues.getOrElseUpdate(obj, obj match {
      case _: GeneratedEnumCompanion[JobStatus] => EnumUtils.enumToTitleIdValues(JobStatusEnum)
      case _                                    => enumToTitleIdValues(obj.values)
    })
  }

}
