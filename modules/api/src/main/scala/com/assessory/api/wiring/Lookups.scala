package com.assessory.api.wiring

import com.assessory.api.critique.CritAllocation
import com.wbillingsley.handy._
import Id._
import appbase._
import com.assessory.api._
import com.wbillingsley.handy.appbase.GroupRole

object Lookups {

  val a = com.wbillingsley.handy.LookUp

  implicit var luUser:LookUp[User, String] = LookUp.fails("User lookup has not been configured")

  implicit var luCReg:LookUp[Course.Reg, String] = LookUp.fails("Course Registration lookup has not been configured")

  implicit var luGReg:LookUp[Group.Reg, String] = LookUp.fails("Group Registration lookup has not been configured")

  implicit var luCourse:LookUp[Course, String] = LookUp.fails("Course lookup has not been configured")

  implicit var luPreenrol:LookUp[Course.Preenrol, String] = LookUp.fails("Preenrol lookup has not been configured")

  implicit var luGroup:LookUp[Group, String] = LookUp.fails("Group lookup has not been configured")

  implicit var luGPreenrol:LookUp[Group.Preenrol, String] = LookUp.fails("GPreenrol lookup has not been configured")

  implicit var luGroupSet:LookUp[GroupSet, String] = LookUp.fails("GroupSet lookup has not been configured")

  implicit var luTask:LookUp[Task, String] = LookUp.fails("Task lookup has not been configured")

  implicit var luTaskOutput:LookUp[TaskOutput, String] = LookUp.fails("TaskOutput lookup has not been configured")

  implicit var luCritAlloc:LookUp[CritAllocation, String] = LookUp.fails("CritAllocation lookup has not been configured")

  var courseRegistrationProvider:RegistrationProvider[Course, CourseRole, HasKind] = new NullRegistrationProvider

  var groupRegistrationProvider:RegistrationProvider[Group, GroupRole, HasKind] = new NullRegistrationProvider

  var idAllocationF:Function0[String] = () => "None"

  def allocateId[T]:Id[T,String] = idAllocationF().asId
}

trait RegistrationProvider[T, R, P <: HasKind] {
  def byUserAndTarget(user:Id[User, String], target:Id[T, String]):RefOpt[Registration[T, R, P]]

  def byUser(user:Id[User, String]):RefMany[Registration[T, R, P]]
}

class NullRegistrationProvider[T, R, P <: HasKind] extends RegistrationProvider[T, R, P] {
  def byUserAndTarget(user:Id[User, String], target:Id[T, String]) = {
    RefOptFailed(new IllegalStateException("No registration provider has been wired up"))
  }

  def byUser(user:Id[User, String]):RefMany[Registration[T, R, P]] = {
    RefManyFailed(new IllegalStateException("No registration provider has been wired up"))
  }
}

