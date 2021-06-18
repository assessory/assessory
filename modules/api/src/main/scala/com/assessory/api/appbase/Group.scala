package com.assessory.api.appbase

import com.wbillingsley.handy.{HasKind, Ids, HasId, Id}
import com.assessory.api.IdSeq

case class Group (

   id:GroupId,

   parent:Option[GroupId] = None,

   course:Option[CourseId] = None,

   set:GroupSetId,

   name:Option[String] = None,

   provenance:Option[String] = None,

   members:Seq[Id[Group.Reg, String]] = Seq.empty,

   created:Long = System.currentTimeMillis

 ) extends HasId[GroupId]

case class GroupId(id:String) extends Id[Group, String]

object Group {
  type Reg = Registration[Group, GroupRole, HasKind]
  type Preenrol = Preenrolment[GroupSet, Group, GroupRole, Group.Reg]
  type PreenrolRow = Preenrolment.Row[Group, GroupRole, Group.Reg]

  case class RegId(id:String) extends Id[Reg, String]
}

case class GroupRole(r:String)

object GroupRole {
  val member = GroupRole("member")
  val roles:Set[GroupRole] = Set(member)
}

