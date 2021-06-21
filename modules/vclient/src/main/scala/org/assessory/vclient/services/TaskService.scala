package org.assessory.vclient.services

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles.{given, _}
import com.assessory.api.appbase._
import com.wbillingsley.handy.{Id, Ids, Latch, EagerLookUpOne, Ref, RefMany, refOps, lazily}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import Ref._

object TaskService {

  given EagerLookUpOne[Id[Task, String], Task] = (r:Id[Task, String]) =>
    latch(r.id).request.map(_.item).toRef

  val cache = mutable.Map.empty[String, Latch[WithPerms[Task]]]

  UserService.self.addListener({ _ => cache.clear() })

  def courseTasks(courseId:Id[Course,String]):Latch[Seq[WithPerms[Task]]] = Latch.lazily(
    Ajax.get(s"/api/course/${courseId.id}/tasks", headers = Map("Accept" -> "application/json")).responseText.flatMap(Pickles.readF[Seq[WithPerms[Task]]])
  )

  def loadId[KK <: String](id:Id[Task,KK]):Future[WithPerms[Task]] = {
    val t = Ajax.get(s"/api/task/${id.id}", headers = Map("Accept" -> "application/json")).responseText
    t.flatMap(Pickles.readF[WithPerms[Task]])
  }

  def latch(s:String):Latch[WithPerms[Task]] = latch(TaskId(s))

  def latch(id:Id[Task,String]):Latch[WithPerms[Task]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(id)))

}
