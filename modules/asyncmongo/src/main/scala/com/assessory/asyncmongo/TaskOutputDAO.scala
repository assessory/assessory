package com.assessory.asyncmongo

import com.assessory.api._
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{TargetB, TaskOutputBodyB, TaskOutputB}
import com.wbillingsley.handy._
import Ref._

object TaskOutputDAO extends DAO(classOf[TaskOutput], "taskOutput", TaskOutputB.read) {

  def saveSafe(c:TaskOutput) = {
    findAndReplace("_id" $eq c.id, TaskOutputB.write(c), upsert=true).toRef
  }

  def updateBody(t:TaskOutput) = updateAndFetch(
    query="_id" $eq t.id,
    update=$set("body" -> TaskOutputBodyB.write(t.body))
  )

  def finalise(t:TaskOutput):Ref[TaskOutput] = updateAndFetch(
    query="_id" $eq t.id,
    update=$set("finalised" -> System.currentTimeMillis())
  ).require

  def byTask(t:Ref[Task]) = {
    for {
      tid <- t.refId
      to <- findMany("task" $eq tid)
    } yield to
  }

  def byTaskAndBy(t:Id[Task,String], by:Target) = {
    findMany (("task" $eq t) and ("by" $eq TargetB.write(by)))
  }

  def byTaskAndAttn(t:Id[Task,String], attn:Target):RefMany[TaskOutput] = {
    println(s"Searching for task $t and attn $attn")

    findMany (("task" $eq t) and ("attn" $eq TargetB.write(attn)))
  }


  def byTaskAndAttn(t:Ref[Task], attn:Target):RefMany[TaskOutput] = {
    for {
      tid <- t.refId
      to <- findMany(bsonDoc("task" -> tid, "attn" -> TargetB.write(attn)))
    } yield to
  }

}
