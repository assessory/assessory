package com.assessory.api

import com.wbillingsley.handy.{Id, HasKind}
import com.assessory.api.appbase._


/**
  * Who an item, usually a task output, is owned by
  */
enum By {
    case ByUser(u:Id[User, String])
    case ByGroup(g:Id[Group, String])
}

sealed trait Target



case class KindedTarget[T <: Target](kind:String, target:T)

case class TargetUser(id:Id[User, String]) extends Target


case class TargetCourseReg(id:Id[Course.Reg, String]) extends Target


case class TargetGroup(id:Id[Group, String]) extends Target


case class TargetTaskOutput(id:Id[TaskOutput, String]) extends Target

