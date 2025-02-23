package org.assessory.vclient.services

import com.assessory.api.*
import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles.*
import com.assessory.api.appbase.*
import com.wbillingsley.handy.{EagerLookUpOne, Id, Ids, Latch, Ref, RefMany, lazily, refOps}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import Ref.*
import com.assessory.api.call.{TaskCall, ReturnTask, StandardReturn}

object TaskService {

  given EagerLookUpOne[Id[Task, String], Task] = (r:Id[Task, String]) =>
    latch(r.id).request.map(_.item).toRef

  val cache = mutable.Map.empty[String, Latch[WithPerms[Task]]]

  UserService.self.addListener({ _ => cache.clear() })

  def courseTasks(courseId:Id[Course,String]):Latch[Seq[WithPerms[Task]]] = Latch.lazily(
    for
      case StandardReturn.ReturnMany(items) <- callClient.makeCall(TaskCall.CourseTasks(CourseId(courseId.id)))
    yield
      for case StandardReturn.ReturnWithPermissions(ReturnTask(t), perms) <- items yield WithPerms(perms, t)
  )

  def loadId(id:TaskId):Future[WithPerms[Task]] = {
    for
      case StandardReturn.ReturnWithPermissions(ReturnTask(t), perms) <- callClient.makeCall(TaskCall.GetTask(id))
    yield
      WithPerms(perms, t)
  }

  def latch(s:String):Latch[WithPerms[Task]] = latch(TaskId(s))

  def latch(id:Id[Task,String]):Latch[WithPerms[Task]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(TaskId(id.id))))

}
