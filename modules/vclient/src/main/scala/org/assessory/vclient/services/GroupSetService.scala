package org.assessory.vclient.services

import com.assessory.api.client.WithPerms
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.appbase.GroupSet
import com.wbillingsley.handy.{Id, Latch}
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object GroupSetService {

  val cache = mutable.Map.empty[String, Latch[WithPerms[GroupSet]]]

  UserService.self.addListener { _ => cache.clear() }

  def loadId[KK <: String](id:Id[GroupSet,KK]):Future[WithPerms[GroupSet]] = {
    Ajax.get(s"/api/groupSet/${id.id}", headers = Map("Accept" -> "application/json")).responseText.flatMap(Pickles.readF[WithPerms[GroupSet]])
  }

  def latch(s:String):Latch[WithPerms[GroupSet]] = cache.getOrElseUpdate(s, Latch.lazily(loadId(s.asId[GroupSet])))

}
