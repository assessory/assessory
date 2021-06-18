package com.assessory.asyncmongo

import com.assessory.api.{given, _}
import com.assessory.api.critique._
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{TargetB, CritAllocationB}
import com.wbillingsley.handy.{Id, Ref, RefFuture, refOps}
import com.assessory.api.appbase.User

object CritAllocationDAO extends DAO(classOf[CritAllocation], "critAllocation", CritAllocationB.read) {

  import DB.given

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
    RefFuture(findAndReplace("_id" $eq c.id, CritAllocationB.write(c), upsert=true))
  }

  def saveNew(gca:CritAllocation) = saveSafe(gca)

  def setOutput(alloc:Id[CritAllocation,String], target:Target, output:Id[TaskOutput,String]) = updateAndFetch(
    query=("_id" $eq alloc) and ("allocation.target" $eq TargetB.write(target)),
    update=$set("allocation.$.critique" -> output)
  )

}
