package org.assessory.vclient.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.Course
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow


object CourseService {

  val cache = mutable.Map.empty[String, Latch[WithPerms[Course]]]

  val myCourses:Latch[Seq[WithPerms[Course]]] = Latch.lazily(
    Ajax.post("/api/course/my", headers = Map("Accept" -> "application/json")).responseText.flatMap(Pickles.readF[Seq[WithPerms[Course]]])
  )
  UserService.self.addListener { _ => myCourses.clear(); cache.clear() }

  def createCourse(c:Course):Future[WithPerms[Course]] = {
    Ajax.post("/api/course/create", data = Pickles.write(c), headers = Map("Accept" -> "application/json")).responseText.flatMap(Pickles.readF[WithPerms[Course]])
  }

  def loadId[KK <: String](id:Id[Course,KK]):Future[WithPerms[Course]] = {
    Ajax.get(s"/api/course/${id.id}", headers = Map("Accept" -> "application/json")).responseText.flatMap(Pickles.readF[WithPerms[Course]])
  }

  def latch(s:String):Latch[WithPerms[Course]] = cache.getOrElseUpdate(s, Latch.lazily(loadId(s.asId[Course])))

  def latch(id:Id[Course,String]):Latch[WithPerms[Course]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(id)))
}
