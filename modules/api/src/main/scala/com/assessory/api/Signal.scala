package com.assessory.api

import com.wbillingsley.handy.{Ids, Id, HasId, HasKind}


/**
  * A signal is information about an assignment that comes in apart from its submission
  */
sealed trait SignalBody(val kind:String) extends HasKind
case class CritiqueAllocated(task:TaskId, taskOutput: TaskOutputId, critiqueTask:TaskId, critique: TaskOutputId) extends SignalBody("CritiqueAllocated")

case class SignalId(id:String) extends Id[Signal, String]


/**
  * 
  *
  * @param task
  * @param taskOutput
  * @param when
  * @param body
  */
case class Signal(
    id: SignalId,
    task: TaskId,
    taskOutput: TaskOutputId,
    created: Long,
    body: SignalBody
) extends HasId[SignalId]

