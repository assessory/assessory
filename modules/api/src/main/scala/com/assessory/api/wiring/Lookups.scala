package com.assessory.api.wiring

import com.assessory.api.critique.CritAllocation
import com.wbillingsley.handy.{HasKind, Id, Ids, LookUp, RefFailed, RefMany, RefManyFailed, RefOpt, RefOptFailed}
import com.assessory.api._
import appbase._
import com.wbillingsley.handy.{EagerLookUpOne, EagerLookUpOpt, EagerLookUpMany}

trait IdLookUp[T] extends LookUp[Id[T, String], T]:
  def many(ids:Seq[Id[T, String]]):RefMany[T]


object Lookups {


  private def fails[T](msg:String):IdLookUp[T] = new IdLookUp[T] {
    override def eagerOne(id: Id[T, String]) = RefFailed(IllegalStateException(msg))
    override def eagerOpt(id: Id[T, String]) = RefOptFailed(IllegalStateException(msg))
    override def many(ids: Seq[Id[T, String]]) = RefManyFailed(IllegalStateException(msg))
  }

  type ELO[T] = EagerLookUpOne[Id[T, String], T]
  type ELOpt[T] = EagerLookUpOpt[Id[T, String], T]
  type ELM[T] = EagerLookUpMany[Seq[Id[T, String]], T]

  extension [T] (ids:Seq[Id[T, String]])(using lm:ELM[T]) {
    def lookUp: RefMany[T] = lm.apply(ids)
  }

  given one[T](using lu:IdLookUp[T]):ELO[T] = lu.eagerOne _
  def opt[T](using lu:IdLookUp[T]):ELOpt[T] = lu.eagerOpt _
  given many[T](using lu:IdLookUp[T]):ELM[T] = lu.many _

  extension [T] (opt:Option[Id[T, String]])(using lu:IdLookUp[T]) {
    def lookUp:RefOpt[T] = RefOpt(opt).flatMap(id => lu.eagerOpt(id))
  }

  implicit var luUser:IdLookUp[User] = fails("User lookup has not been configured")

  implicit var luCReg:IdLookUp[Course.Reg] = fails("Course Registration lookup has not been configured")

  implicit var luGReg:IdLookUp[Group.Reg] = fails("Group Registration lookup has not been configured")

  implicit var luCourse:IdLookUp[Course] = fails("Course lookup has not been configured")

  implicit var luPreenrol:IdLookUp[Course.Preenrol] = fails("Preenrol lookup has not been configured")

  implicit var luGroup:IdLookUp[Group] = fails("Group lookup has not been configured")

  implicit var luGPreenrol:IdLookUp[Group.Preenrol] = fails("GPreenrol lookup has not been configured")

  implicit var luGroupSet:IdLookUp[GroupSet] = fails("GroupSet lookup has not been configured")

  implicit var luTask:IdLookUp[Task] = fails("Task lookup has not been configured")

  implicit var luTaskOutput:IdLookUp[TaskOutput] = fails("TaskOutput lookup has not been configured")

  implicit var luCritAlloc:IdLookUp[CritAllocation] = fails("CritAllocation lookup has not been configured")

  var courseRegistrationProvider:RegistrationProvider[Course, CourseRole, HasKind] = new NullRegistrationProvider

  var groupRegistrationProvider:RegistrationProvider[Group, GroupRole, HasKind] = new NullRegistrationProvider

  var idAllocationF:Function0[String] = () => "None"
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

