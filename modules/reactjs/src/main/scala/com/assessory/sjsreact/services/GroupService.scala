package com.assessory.sjsreact.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import Pickles._
import com.wbillingsley.handy.{Ids, Id, Latch}
import Id._
import Ids._
import com.wbillingsley.handy.appbase.{Group, Course}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Success, Failure}

object GroupService {

  val cache = mutable.Map.empty[String, Latch[WithPerms[Group]]]

  val myGroups:Latch[Seq[WithPerms[Group]]] = Latch.lazily(
    Ajax.post(s"/api/group/my", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[WithPerms[Group]]])
  )
  UserService.self.addListener { _ => myGroups.clear(); cache.clear() }

  def myGroupsInCourse(courseId:Id[Course,String]):Latch[Seq[WithPerms[Group]]] = Latch.lazily(
    Ajax.post(s"/api/course/${courseId.id}/group/my", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[WithPerms[Group]]])
  )

  def loadId[KK <: String](id:Id[Group,KK]):Future[WithPerms[Group]] = {
    Ajax.get(s"/api/group/${id.id}", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[WithPerms[Group]])
  }

  def latch(s:String):Latch[WithPerms[Group]] = latch(s.asId)

  def latch(id:Id[Group,String]):Latch[WithPerms[Group]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(id)))

  def preload[KK <: String](ids:Ids[Group,KK]):Future[Seq[WithPerms[Group]]] = {
    val idStrings:Seq[String] = ids.ids
    val missing = idStrings.filterNot(id => cache.contains(id))
    val promiseMap = (for (id <- missing) yield {
      val p = Promise[WithPerms[Group]]()
      cache.put(id, Latch.lazily(p.future))
      id -> p
    }).toMap

    val loading = Ajax.post("/api/group/findMany", Pickles.write(missing.asIds[Group]), headers=AJAX_HEADERS)
      .responseText.flatMap(Pickles.readF[Seq[WithPerms[Group]]])

    loading.andThen {
      case Success(seq) => for (g <- seq) promiseMap(g.item.id.id).complete(Success(g))
      case Failure(t) => for (p <- promiseMap.values) p.complete(Failure(t))
    }
  }

  def loadIds[KK <: String](ids:Ids[Group,KK]):Future[Seq[WithPerms[Group]]] = {
    preload(ids)
    ids.ids.map(id => cache(id).request).foldLeft(Future.successful(mutable.Buffer.empty[WithPerms[Group]])) { case (fbuf, f) =>
      for (buf <- fbuf; v <- f) yield { buf.append(v); buf }
    }
  }

  def latch(ids:Ids[Group,String]):Latch[Seq[WithPerms[Group]]] = Latch.lazily(loadIds(ids))


}
