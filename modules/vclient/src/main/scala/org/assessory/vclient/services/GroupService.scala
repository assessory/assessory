package org.assessory.vclient.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles.{given, _}
import com.assessory.api.appbase.*
import com.assessory.api.call.{GroupCall, ReturnGroup, ReturnTask, StandardReturn}
import com.wbillingsley.handy.{EagerLookUpOne, Id, Ids, Latch, Ref, RefMany, refOps}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object GroupService {

  given EagerLookUpOne[Id[Group, String], Group] = (r: Id[Group, String]) =>
    latch(r.id).request.map(_.item).toRef

  val cache = mutable.Map.empty[GroupId, Latch[WithPerms[Group]]]

  val myGroups:Latch[Seq[WithPerms[Group]]] = Latch.lazily(
    for
      StandardReturn.ReturnMany(items) <- callClient.makeCall(GroupCall.MyGroups)
    yield
      for StandardReturn.ReturnWithPermissions(ReturnGroup(t), perms) <- items yield WithPerms(perms, t)
  )
  UserService.self.addListener { _ => myGroups.clear(); cache.clear() }

  def myGroupsInCourse(courseId:Id[Course,String]):Latch[Seq[WithPerms[Group]]] = Latch.lazily(
    for
      StandardReturn.ReturnMany(items) <- callClient.makeCall(GroupCall.MyGroupsInCourse(CourseId(courseId.id)))
    yield
      for StandardReturn.ReturnWithPermissions(ReturnGroup(t), perms) <- items yield WithPerms(perms, t)
  )

  def loadId(id:GroupId):Future[WithPerms[Group]] = {
    for
      StandardReturn.ReturnWithPermissions(ReturnGroup(g), perms) <- callClient.makeCall(GroupCall.GetGroup(id))
    yield
      WithPerms(perms, g)
  }

  def latch(s:String):Latch[WithPerms[Group]] = latch(GroupId(s))

  def latch(id:GroupId):Latch[WithPerms[Group]] = cache.getOrElseUpdate(id, Latch.lazily(loadId(id)))

  def preload(ids:Seq[GroupId]):Unit = {
    val missing = ids.filterNot(id => cache.contains(id))
    val missingItems =
      for
        StandardReturn.ReturnMany(items) <- callClient.makeCall(GroupCall.GetManyGroups(missing))
      yield
        for StandardReturn.ReturnWithPermissions(ReturnGroup(g), perms) <- items yield WithPerms(perms, g)

    def find(id:GroupId):Future[WithPerms[Group]] =
      missingItems.flatMap { items =>
        items.find(_.item.id == id) match {
          case Some(item) => Future.successful(item)
          case None => Future.failed(new NoSuchElementException)
        }
      }

    for id <- missing do cache.put(id, Latch.lazily(find(id)))
  }

  def loadIds(ids:Seq[GroupId]):Future[Seq[WithPerms[Group]]] = {
    preload(ids)
    // FIXME: this is a mess
    val fb = ids.map(id => cache(id).request).foldLeft(Future.successful(mutable.Buffer.empty[WithPerms[Group]])) { case (fbuf, f) =>
      for (buf <- fbuf; v <- f) yield { buf.append(v); buf }
    }
    fb.map(_.toSeq)
  }

  def latch(ids:Seq[GroupId]):Latch[Seq[WithPerms[Group]]] = Latch.lazily(loadIds(ids))


}
