package org.assessory.vclient.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import com.wbillingsley.handy.appbase.{Course, Group}
import com.wbillingsley.handy.{Id, Ids, Latch, LookUp, Ref, RefMany}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import Ref._

object GroupService {

  implicit val lookup = new LookUp[Group, String] {
    override def one[KK <: String](r: Id[Group, KK]): Ref[Group] = {
      latch(r.id).request.map(_.item).toRef
    }

    override def many[KK <: String](r: Ids[Group, KK]): RefMany[Group] = {
      for {
        gs <- loadIds(r).toRef
        g <- gs.toRefMany
      } yield g.item
    }
  }

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
    // FIXME: this is a mess
    val fb = ids.ids.map(id => cache(id).request).foldLeft(Future.successful(mutable.Buffer.empty[WithPerms[Group]])) { case (fbuf, f) =>
      for (buf <- fbuf; v <- f) yield { buf.append(v); buf }
    }
    fb.map(_.toSeq)
  }

  def latch(ids:Ids[Group,String]):Latch[Seq[WithPerms[Group]]] = Latch.lazily(loadIds(ids))


}
