package com.assessory.asyncmongo

import com.assessory.api.{given, _}
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{TaskBodyB, TaskB}
import com.wbillingsley.handy.{Ref, refOps}
import com.assessory.api.appbase.Course
import Ref._

object TaskDAO extends DAO(classOf[Task], "task", TaskB.read) {

  def saveSafe(c:Task) = {
    findAndReplace("_id" $eq c.id, TaskB.write(c), upsert=true).toRef
  }

  def updateBody(t:Task) = updateAndFetch(
    query="_id" $eq t.id,
    update=$set("body" -> TaskBodyB.write(t.body))
  )

  def byName(c:Ref[Course], n:String) = {
    for {
      cid <- c.refId
      t <- findOne(("course" $eq cid) and ("details.name" $eq n))
    } yield t
  }


  def byCourse(c:Ref[Course]) = {
    for {
      cid <- c.refId
      t <- findMany("course" $eq cid)
    } yield t
  }

}

