package com.assessory.api

import com.wbillingsley.handy.{Id, HasId, HasKind}

trait TaskOutputBody extends HasKind
case class KindedTaskOutputBody[T <: TaskOutputBody](kind:String, body:T)


object EmptyTaskOutputBody extends TaskOutputBody {
  val kind = "Empty"
}

case class TaskOutput (

  id:TaskOutputId,

  task:TaskId,

  by:Target,

  attn:Seq[Target] = Seq.empty,

  body: TaskOutputBody = EmptyTaskOutputBody,

  created:Long = System.currentTimeMillis,

  finalised:Option[Long] = None,

  updated:Long = System.currentTimeMillis
) extends HasId[TaskOutputId]

case class TaskOutputId(id:String) extends Id[TaskOutput, String]
