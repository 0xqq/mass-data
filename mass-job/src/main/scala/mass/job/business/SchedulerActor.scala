package mass.job.business

import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, Status, SupervisorStrategy}
import helloscala.common.exception.{HSBadRequestException, HSException, HSNotFoundException}
import helloscala.common.util.StringUtils
import mass.job.JobSystem

class SchedulerActor(
    jobSystem: JobSystem,
    propsList: Iterable[(Props, Symbol)]
) extends Actor {
  private var actors = Map[Symbol, ActorRef]()

  override def preStart(): Unit =
    actors = propsList.map { case (props, symbol) => symbol -> context.actorOf(props, symbol.name) }.toMap

  override def postStop(): Unit = {
    actors.valuesIterator.foreach(actor => context.stop(actor))
    actors = Map()
  }

  override def supervisorStrategy: SupervisorStrategy = {
    import SupervisorStrategy._
    OneForOneStrategy() {
      case _: HSException => Resume
      case other          => defaultDecider(other)
    }
  }

  override def receive: Receive = {
    case (name: Symbol, msg) => sendMessage(name, msg)
    case msg =>
      StringUtils.extractFirstName(msg) match {
        case Some(name) => sendMessage(Symbol(name), msg)
        case _          => sender() ! Status.Failure(HSBadRequestException(s"服务未找到，发送消息为：$msg"))
      }
  }

  private def sendMessage(name: Symbol, msg: Any): Unit = {
    actors.get(name) match {
      case Some(actor) => actor forward msg
      case None        => sender() ! Status.Failure(HSNotFoundException(s"actor: $name 未找到，发送消息为：$msg"))
    }
  }
}

object SchedulerActor {
  val name = 'scheduler

  def props(jobSystem: JobSystem, propsList: Iterable[(Props, Symbol)]): Props = {
    require(propsList.groupBy(_._2).size == propsList.size, "propsList有重复的名字")
    Props(new SchedulerActor(jobSystem, propsList))
  }

}
