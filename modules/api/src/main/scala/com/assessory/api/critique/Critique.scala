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

sealed trait TargetType
case class TTGroups(set:Id[GroupSet, String]) extends TargetType
case class TTOutputs(task:Id[Task, String]) extends TargetType
case object TTSelf extends TargetType

sealed trait CritTargetStrategy
case class KindedTargetStrategy[T <: CritTargetStrategy](kind:String, strategy:T)

case class TargetMyStrategy(
  task: Id[Task,String],
  what: TargetType,
  number: Option[Int]
) extends CritTargetStrategy

case class AllocateStrategy(
  what: TargetType,
  number: Int
) extends CritTargetStrategy

case class AnyStrategy(
  what: TargetType,
  number: Int
) extends CritTargetStrategy



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

  completeBy: Target,

  allocation: Seq[AllocatedCrit] = Seq.empty

) extends HasId[CritAllocationId]

case class CritAllocationId(id:String) extends Id[CritAllocation, String]