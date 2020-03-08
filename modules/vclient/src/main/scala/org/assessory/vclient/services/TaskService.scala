package org.assessory.vclient.services

import com.assessory.api.{Task, TaskOutput}
import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.appbase.Course
import com.wbillingsley.handy.{Id, Ids, Latch, LookUp, Ref, RefMany}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import Ref._

object TaskService {


  implicit val lookup = new LookUp[Task, String] {
    override def one[KK <: String](r: Id[Task, KK]): Ref[Task] = {
      latch(r.id).request.map(_.item).toRef
    }

    override def many[KK <: String](r: Ids[Task, KK]): RefMany[Task] = ???
  }

  val cache = mutable.Map.empty[String, Latch[WithPerms[Task]]]

  UserService.self.addListener({ _ => cache.clear() })

  def courseTasks(courseId:Id[Course,String]):Latch[Seq[WithPerms[Task]]] = Latch.lazily(
    Ajax.get(s"/api/course/${courseId.id}/tasks", headers = Map("Accept" -> "application/json")).responseText.flatMap(Pickles.readF[Seq[WithPerms[Task]]])
  )

  def loadId[KK <: String](id:Id[Task,KK]):Future[WithPerms[Task]] = {
    val t = Ajax.get(s"/api/task/${id.id}", headers = Map("Accept" -> "application/json")).responseText
    t.flatMap(Pickles.readF[WithPerms[Task]])
  }

  def latch(s:String):Latch[WithPerms[Task]] = latch(s.asId)

  def latch(id:Id[Task,String]):Latch[WithPerms[Task]] = cache.getOrElseUpdate(id.id, Latch.lazily(loadId(id)))

}
