package org.assessory.vclient.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.assessory.clientpickle.CallPickles._
import com.wbillingsley.handy.{Id, Latch, lazily}
import com.assessory.api.appbase._
import com.assessory.api.call._
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object CourseService {

  val cache = mutable.Map.empty[String, Latch[WithPerms[Course]]]

  val myCourses:Latch[Seq[WithPerms[Course]]] = Latch.lazily(
    for
      case StandardReturn.ReturnMany(entries) <- callClient.makeCall(CourseCall.MyCourses)
    yield
      for case StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- entries yield WithPerms(perms, c)
  )

  UserService.self.addListener { _ => myCourses.clear(); cache.clear() }

  def createCourse(c:Course):Future[WithPerms[Course]] = {
    for
      case StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- callClient.makeCall(CourseCall.CreateCourse(c))
    yield
      WithPerms(perms, c)
  }

  def loadId(id:CourseId):Future[WithPerms[Course]] = {
    for
      case StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- callClient.makeCall(CourseCall.GetCourse(id))
    yield
      WithPerms(perms, c)
  }

  def latch(s:String):Latch[WithPerms[Course]] = cache.getOrElseUpdate(s, Latch.lazily(loadId(CourseId(s))))

  def latch(id:Id[Course,String]):Latch[WithPerms[Course]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(CourseId(id.id))))
}
