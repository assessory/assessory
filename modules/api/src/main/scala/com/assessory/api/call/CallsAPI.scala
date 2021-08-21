package com.assessory.api.call

import com.assessory.api.{Task, TaskId, TaskOutput, TaskOutputId}
import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{ActiveSession, Course, CourseId, Group, GroupId, GroupRole, GroupSet, GroupSetId, User, UserId}
import com.assessory.api.Target
import com.assessory.api.client.WithPerms

class CallsAPI {

}

sealed trait Call

/**
 * Session calls are the only ones that work directly with sessions. They are handled directly in the call client,
 * to prevent hijacking of the session key
 */
enum SessionCall extends Call {
  case GetSession
  case WithSession(a:ActiveSession, c:Call)
  case Register(email:String, password:String, session:ActiveSession)
  case Login(email:String, password:String, session:ActiveSession)
  case Logout(session:ActiveSession)
}

enum UserCall extends Call {
  case WhoAmI
  case GetUser(id:UserId)
}

enum CourseCall extends Call {
  case GetCourse(id:CourseId)
  case ByShortName(sn:String)
  case CreateCourse(c:Course)
  case MyCourses
}

enum TaskCall extends Call {
  case GetTask(id:TaskId)
  case ByName(cid:CourseId, n:String)
  case CreateTask(t:Task)
  case CourseTasks(cid:CourseId)
}

enum GroupSetCall extends Call {
  case GetGroupSet(id:GroupSetId)
  case ByName(c:CourseId, n:String)
  case CreateGroupSet(gs:GroupSet)
}

enum GroupCall extends Call {
  case GetGroup(id:GroupId)
  case GetManyGroups(ids:Seq[GroupId])
  case MyGroups
  case MyGroupsInCourse(courseId:CourseId)
  case CreateGroupsFromCsv(setId: GroupSetId, csv: String)
  case CreateGroup(g:Group)
  case AddGroupReg(gr:Group.Reg)
}

enum TaskOutputCall extends Call {
  case GetTaskOutput(id:TaskOutputId)
  case CreateTaskOutput(to:TaskOutput)
  case MyOutputs(task:TaskId)
  case AllOutputs(task:TaskId)
  case UpdateBody(taskOutput: TaskOutput)
  case Finalise(taskOutput: TaskOutput)
}

enum CritiqueCall extends Call {
  case MyAllocations(taskId: TaskId)
  case FillMyTaskOutputs(taskId: TaskId)
  case FindOrCreateCritique(taskId: TaskId, target:Target)
}

trait Return
case class ReturnSession(a:ActiveSession) extends Return
case class ReturnUser(u:User) extends Return
case class ReturnCourse(c:Course) extends Return
case class ReturnTask(t:Task) extends Return
case class ReturnTarget(t:Target) extends Return
case class ReturnTaskOutput(t:TaskOutput) extends Return
case class ReturnGroupSet(gs:GroupSet) extends Return
case class ReturnGroup(g:Group) extends Return
case class ReturnGroupReg(gr:Group.Reg) extends Return
case class ReturnGroupsData(data:Seq[(Group, Seq[String])]) extends Return

enum StandardReturn extends Return {
  case ReturnNone
  case ReturnWithPermissions(r: Return, perms: Map[String, Boolean]) // TODO: Remove. Just use "ask" at the client
  case ReturnMany(r:Seq[Return])
}