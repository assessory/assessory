package com.assessory.api.call

import com.assessory.api.Task
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{ActiveSession, Course, Group, GroupRole, GroupSet, User}

class CallsAPI {

}


trait Call
case object GetSession extends Call  // TODO: replace with JWT
case class WithSession(a:ActiveSession, c:Call) extends Call
case class Register(email:String, password:String, session:ActiveSession) extends Call
case class Login(email:String, password:String, session:ActiveSession) extends Call

case class CreateCourse(c:Course) extends Call
case class CreateTask(t:Task) extends Call

case class CreateGroupSet(gs:GroupSet) extends Call
case class CreateGroupsFromCsv(setId: Id[GroupSet, String], csv: String) extends Call

case class CreateGroup(g:Group) extends Call
case class AddGroupReg(gr:Group.Reg) extends Call

trait Return
case class ReturnSession(a:ActiveSession) extends Return
case class ReturnUser(u:User) extends Return
case class ReturnCourse(c:Course) extends Return
case class ReturnTask(t:Task) extends Return
case class ReturnGroupSet(gs:GroupSet) extends Return
case class ReturnGroupReg(gr:Group.Reg) extends Return
case class ReturnGroupsData(data:Seq[(Group, Seq[String])]) extends Return

