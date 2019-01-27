package com.assessory.asyncmongo

import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{ActiveSessionB, IdentityB, UserB}
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy.appbase.{ActiveSession, Identity, User, Course}
import com.wbillingsley.handy.{LazyId, Ref, RefOpt, Refused}
import com.wbillingsley.handyplay.UserProvider

object UserDAO extends DAO(classOf[User], "assessoryUser", UserB.read) with UserProvider[User] with com.wbillingsley.handy.user.UserDAO[User, Identity] {

  def unsaved = User(id = allocateId.asId)

  def saveSafe(c:User) = {
    findAndReplace("_id" $eq c.id, UserB.write(c), upsert=true).toRef
  }

  /**
   * Saves the user's details
   */
  def saveDetails(u:User) = updateAndFetch(
    query=idIs(u.id),
    update=$set(
      "name" -> u.name,
      "nickname" -> u.nickname,
      "avatar" -> u.avatar,
      "created" -> u.created
    )
  )

  /**
   * Save a new user. This should only be used for new users because it overwrites
   * sessions and identities.
   */
  def saveNew(u:User) = saveSafe(u)

  /**
   * Adds an identity to this user
   */
  def pushIdentity(ru:Ref[User], i:Identity):Ref[User] = {
    for {
      uid <- ru.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,
        update = $push("identities" -> IdentityB.write(i))
      ).require
    } yield u
  }

  /** Adds a session to this user. Typically this happens at login. */
  def pushSession(ru:Ref[User], as:ActiveSession):Ref[User] = {
    for {
      uid <- ru.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,
        update = $push("activeSessions" -> ActiveSessionB.write(as))
      ).require
    } yield u
  }

  def deleteSession(ru:Ref[User], as:ActiveSession):Ref[User] = {
    for {
      uid <- ru.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,
        update = $pull("activeSessions" -> bsonDoc("key" -> as.key))
      ).require
    } yield u
  }

  override def bySessionKey(sessionKey:String):RefOpt[User] = {
    findOne(query="activeSessions.key" $eq sessionKey)
  }

  override def byIdentity(service:String, id:String):RefOpt[User] = {
    findOne(query=("identities.service" $eq service) and ("identities.value" $eq id))
  }

  override def byIdentity(i:Identity):RefOpt[User] = bySocialIdOrUsername(i.service, i.value, i.username)

  def bySocialIdOrUsername(service:String, optId:Option[String], optUserName:Option[String] = None):RefOpt[User] = {

    def byId(service:String, oid:Option[String]) = for {
      id <- oid.toRef
      u <- findOne(query=("identities.service" $eq service) and ("identities.value" $eq id))
    } yield u

    def byUsername(service:String, oun:Option[String]) = for {
      n <- oun.toRef
      u <- findOne(query=("identities.service" $eq service) and ("identities.username" $eq n))
    } yield u

    byId(service, optId) orElse byUsername(service, optUserName)
  }

  def byUsername(u:String) = findOne("pwlogin.username" $eq u)

  def byEmail(e:String) = findOne("pwlogin.email" $eq e)

  def byUsernameAndPassword(username:String, password:String) = {
    for (
      user <- byUsername(username) if checkPassword(user.pwlogin, password)
    ) yield user
  }

  def byEmailAndPassword(email:String, password:String) = {
    for (
      user <- byEmail(email) if checkPassword(user.pwlogin, password)
    ) yield user
  }

  def byCourse(c:Ref[Course]) = {
    c.refId map ("registrations.course" $eq _) flatMap findMany
  }

  def addSession(user: Ref[User], session: ActiveSession): Ref[User] = pushSession(user, session)

  def removeIdentity(user: Ref[User], identity: Identity): Ref[User] = {
    for {
      uid <- user.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,
        update = $pull("identities" -> IdentityB.write(identity)) // TODO: deal with mismatch id/value
      ).require
    } yield u
  }

  def removeSession(user: Ref[User], sessionKey: String): Ref[User] = deleteSession(user, ActiveSession(key=sessionKey, ip=""))

  def addIdentity(user: Ref[User], identity: Identity): Ref[User] = pushIdentity(user, identity)
}
