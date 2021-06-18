package com.assessory.api

import com.wbillingsley.handy.{Id, HasKind}
import com.assessory.api.appbase._

sealed trait Target



case class KindedTarget[T <: Target](kind:String, target:T)

case class UnrecognisedT(original:String) extends Target

case class TargetUser(id:Id[User, String]) extends Target


case class TargetCourseReg(id:Id[Course.Reg, String]) extends Target


case class TargetGroup(id:Id[Group, String]) extends Target


case class TargetTaskOutput(id:Id[TaskOutput, String]) extends Target

