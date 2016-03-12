package com.assessory.asyncmongo

import com.assessory.api._
import com.assessory.api.critique._
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{TargetB, CritAllocationB}
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.User
import Ref._

object CritAllocationDAO extends DAO(classOf[CritAllocation], "critAllocation", CritAllocationB.read) {

  def byTask(t:Ref[Task]) = {
    for {
      tId <- t.refId
      d <- findMany("task" $eq tId)
    } yield d
  }

  def byUserAndTask(u:Ref[User], t:Ref[Task]) = {
    for {
      uId <- u.refId
      tId <- t.refId
      d <- findMany(bsonDoc("task" -> tId, "completeBy.kind" -> "User", "completeBy.id" -> uId))
    } yield d
  }

  def saveSafe(c:CritAllocation) = {
    findAndReplace("_id" $eq c.id, CritAllocationB.write(c), upsert=true).toRef
  }

  def saveNew(gca:CritAllocation) = saveSafe(gca)

  def setOutput(alloc:Id[CritAllocation,String], target:Target, output:Id[TaskOutput,String]) = updateAndFetch(
    query=("_id" $eq alloc) and ("allocation.target" $eq TargetB.write(target)),
    update=$set("allocation.$.critique" -> output)
  )

}
