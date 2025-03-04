package com.assessory.api

import com.wbillingsley.handy.{Id, HasId, HasKind}

/**
  * A TaskRecord holds details of a group or student's work in a task.
  * This is generated data, rather than original data. For instance, when a 
  * `TaskOutput` is created or a `Signal` is received, the `TaskRecord` might 
  * also be updated.
  * 
  */
case class TaskRecord (

  task:TaskId,

  by:By,

  knownOutputs:Seq[TaskOutputId]

)

