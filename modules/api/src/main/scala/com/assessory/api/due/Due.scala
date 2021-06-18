package com.assessory.api.due

import com.assessory.api.appbase.{Group, GroupId}
import com.assessory.api.IdSeq
import com.wbillingsley.handy.{Id, Ids}

sealed trait Due {
  /**
    * Given a student's group memberships, when is this due?
    */
  def due(groups:Seq[Id[Group, String]]):Option[Long]
}

case class DueDate(time:Long) extends Due {
  def due(groups:Seq[Id[Group, String]]) = Some(time)
}

case class DuePerGroup(times:Map[Id[Group, String], Long]) extends Due {
  def due(groups:Seq[Id[Group, String]]) = {
    val i = times.keySet.intersect(groups.toSet).headOption
    i.map(times.apply)
  }
}

case object NoDue extends Due {
  def due(groups:Seq[Id[Group, String]]) = None
}