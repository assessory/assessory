package com.assessory.asyncmongo

import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.GroupSetB
import com.wbillingsley.handy.{Ref, refOps}
import com.assessory.api.appbase.{Course, Group, GroupSet}
import com.assessory.api.given


object GroupSetDAO extends DAO(classOf[GroupSet], "groupSet", GroupSetB.read) {

  /**
   * Saves the user's details
   */
  def saveDetails(g:GroupSet) = updateAndFetch(
    query= "_id" $eq g.id,
    update=$set(
      "name" -> g.name,
      "description" -> g.description
    )
  )

  def saveSafe(c:GroupSet) = {
    findAndReplace("_id" $eq c.id, GroupSetB.write(c), upsert=true).toRef
  }

  /**
   * Save a new user. This should only be used for new users because it overwrites
   * sessions and identities.
   */
  def saveNew(c:GroupSet) = saveSafe(c)


  def setPreenrol(gs:Ref[GroupSet], gp:Ref[Group.Preenrol]) = {
    for {
      gsid <- gs.refId
      gpid <- gp.refId
      gs <- updateAndFetch(
        query="_id" $eq gsid,
        update=$set("preenrol" -> gpid)
      )
    } yield gs
  }

  def byCourse(c:Ref[Course]) = for {
    cid <- c.refId
    gs <- findMany("course" $eq cid)
  } yield gs


}
