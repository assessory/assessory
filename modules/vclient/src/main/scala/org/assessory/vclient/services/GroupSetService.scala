package org.assessory.vclient.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles.*
import com.wbillingsley.handy.Id.*
import com.assessory.api.appbase.{CourseId, GroupSet, GroupSetId}
import com.assessory.api.call.{GroupSetCall, ReturnGroupSet, StandardReturn}
import com.wbillingsley.handy.{Id, Latch}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object GroupSetService {

  val cache = mutable.Map.empty[String, Latch[WithPerms[GroupSet]]]

  UserService.self.addListener { _ => cache.clear() }

  def loadId(id:GroupSetId):Future[WithPerms[GroupSet]] = {
    for
      case StandardReturn.ReturnWithPermissions(ReturnGroupSet(gs), perms) <- callClient.makeCall(GroupSetCall.GetGroupSet(id))
    yield
      WithPerms(perms, gs)
  }

  def latch(s:String):Latch[WithPerms[GroupSet]] = cache.getOrElseUpdate(s, Latch.lazily(loadId(GroupSetId(s))))

  def latch(id:Id[GroupSet,String]):Latch[WithPerms[GroupSet]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(GroupSetId(id.id))))

  def groupSetsInCourse(c:CourseId):Future[Seq[GroupSet]] =
    for case StandardReturn.ReturnMany(items) <- callClient.makeCall(GroupSetCall.ByCourse(c)) yield
      for case ReturnGroupSet(g) <- items yield g

}
