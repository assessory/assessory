package com.assessory.api

import com.assessory.api.due.{NoDue, Due}
import com.wbillingsley.handy.{Ids, Id, HasId, HasKind}
import com.assessory.api.appbase._

/**
  * There could be many different kinds of task -- questionnaires to fill, essays to write. We implement these
  * as `TaskBody`s
  */
trait TaskBody

case class KindedTaskBody[T <: TaskBody](kind:String, taskBody:T)

/**
  * A sentinel for when a TaskBody has not been created
  */
object EmptyTaskBody extends TaskBody

/**
  * A task is something that someone has to do.
  */
case class Task(

  id: TaskId,

  course: CourseId,

  details: TaskDetails = new TaskDetails(),

  body: TaskBody = EmptyTaskBody

) extends HasId[TaskId]

case class TaskId(id:String) extends Id[Task, String]

case class TaskDetails (

  name:Option[String] = None,

  description:Option[String] = None,

  created: Long = System.currentTimeMillis,

  groupSet: Option[Id[GroupSet, String]] = None,

  individual: Boolean = true,

  published: Due = NoDue,

  restrictions: Seq[TaskRule] = Seq.empty,

  open: Due = NoDue,

  due: Due = NoDue,

  closed: Due = NoDue

)

sealed trait TaskRule
case class MustHaveFinished(task: Id[Task, String]) extends TaskRule



