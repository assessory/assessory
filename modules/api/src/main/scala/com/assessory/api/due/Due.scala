package com.assessory.api.due

import com.wbillingsley.handy.appbase.Group
import com.wbillingsley.handy.{Id, Ids}

sealed trait Due {
  /**
    * Given a student's group memberships, when is this due?
    */
  def due(groups:Ids[Group,String]):Option[Long]
}

case class DueDate(time:Long) extends Due {
  def due(groups:Ids[Group,String]) = Some(time)
}

case class DuePerGroup(times:Map[Id[Group, String], Long]) extends Due {
  def due(groups:Ids[Group,String]) = {
    val i = times.keySet.intersect(groups.toSeqId.toSet).headOption
    i.map(times.apply)
  }
}

case object NoDue extends Due {
  def due(groups:Ids[Group,String]) = None
}