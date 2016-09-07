package com.assessory.asyncmongo

import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.GroupB
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase._
import Ref._
import org.mongodb.scala.bson.{BsonArray, BsonString}

object GroupDAO extends DAO(classOf[Group], "assessoryGroup", GroupB.read) {

  def saveSafe(c:Group) = {
    findAndReplace("_id" $eq c.id, GroupB.write(c), upsert=true).toRef
  }

  /**
   * Save a new group. This should only be used for new groups because it overwrites
   * members.
   */
  def saveNew(c:Group) = saveSafe(c)

  def addMember(g:Ref[Group], u:Ref[User]) = {
    for {
      gid <- g.refId
      uid <- u.refId
      reg <- RegistrationDAO.group.register(uid, gid, Set(GroupRole.member), EmptyKind)
      query = "_id" $eq gid
      update = $addToSet("members" -> reg.id)
      updated <- updateAndFetch(query, update)
    } yield updated
  }

  def byCourse(c:Ref[Course]) = {
    for {
      cid <- c.refId
      g <- findMany("course" $eq cid)
    } yield g
  }

  def byCourseAndName(c:Ref[Course], name:String) = {
    for {
      cid <- c.refId
      g <- findOne(("course" $eq cid) and ("name" $eq name))
    } yield g
  }

  def bySet(gsId:Id[GroupSet,String]) = findMany("set" $eq gsId)

  def bySetAndUser(gsId:Id[GroupSet,String], u:Id[User,String]) = {
    for {
      s <- bySet(gsId).collect
      r <- RegistrationDAO.group.byUserAndTargets(u, s.map(_.id))
      g <- r.target.lazily
    } yield {
      g
    }
  }

  def byNames(gsId:Id[GroupSet, String], names:Set[String]) = {
    findMany(bsonDoc("set" -> gsId, "name" -> $in(BsonArray(names.toSeq.map(BsonString(_))))))
  }

  def byNames(names:Set[String]) = findMany(bsonDoc("name" -> $in(BsonArray(names.toSeq.map(BsonString(_))))))

}
