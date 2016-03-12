package com.assessory.api

import com.wbillingsley.handy.{Id, HasKind}
import com.wbillingsley.handy.appbase._

sealed trait Target {
  val id: Id[_, String]
}

case class UnrecognisedT(id:Id[Nothing, String], original:String) extends Target

case class TargetUser(id:Id[User, String]) extends Target

case class TargetCourseReg(id:Id[Course.Reg, String]) extends Target

case class TargetGroup(id:Id[Group, String]) extends Target

case class TargetTaskOutput(id:Id[TaskOutput, String]) extends Target
