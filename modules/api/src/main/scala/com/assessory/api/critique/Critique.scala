package com.assessory.api.critique

import com.wbillingsley.handy.{Id, HasId}
import com.assessory.api._
import com.assessory.api.{Task, TaskId}
import com.assessory.api.question.{Answer, Question}
import com.assessory.api.appbase.GroupSet

/**
  * A critique is posted in response to something
  */
case class Critique(
  target: Target,

  task: TaskOutputBody
) extends TaskOutputBody {
  val kind = CritiqueTask.kind
}

/**
  * What sort of thing are we critiquing?
  */
sealed trait TargetType
case class TTGroups(set:Id[GroupSet, String]) extends TargetType
case class TTOutputs(task:Id[Task, String]) extends TargetType
case object TTSelf extends TargetType

/**
  * How to allocate the items to be critiqued
  */
sealed trait CritTargetStrategy
case class KindedTargetStrategy[T <: CritTargetStrategy](kind:String, strategy:T)

/**
  * Let me critique items that are addressed to me
  * (Used in reverse critiques)
  *
  * @param task
  * @param what
  * @param number
  */
case class TargetMyStrategy(
  task: Id[Task,String],
  what: TargetType,
  number: Option[Int]
) extends CritTargetStrategy


/**
  * Allocate some number ot items to be critiqued
  *
  * @param what
  * @param number
  */
case class AllocateStrategy(
  what: TargetType,
  number: Int
) extends CritTargetStrategy


/**
  * A critique selects existing (something)s and asks you to perform a task on them
  *
  * @param strategy
  * @param task
  */
case class CritiqueTask (
  strategy: CritTargetStrategy,
  task: TaskBody
) extends TaskBody {
  val kind = CritiqueTask.kind
}

object CritiqueTask {
  val kind = "Critique"
}


case class AllocatedCrit(
  target: Target,

  critique: Option[Id[TaskOutput, String]] = None
)


case class CritAllocation(

  id: CritAllocationId,

  task: TaskId,

  completeBy: By,

  allocation: Seq[AllocatedCrit] = Seq.empty

) extends HasId[CritAllocationId]

case class CritAllocationId(id:String) extends Id[CritAllocation, String]