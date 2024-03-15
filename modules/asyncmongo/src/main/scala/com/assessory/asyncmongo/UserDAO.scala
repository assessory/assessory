package com.assessory.asyncmongo

import com.assessory.datalayer

import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{ActiveSessionB, IdentityB, UserB}
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.{Ref, RefOpt, refOps}
import com.assessory.api.appbase.{ActiveSession, Course, Identity, PasswordLogin, User, UserId}
import com.assessory.api.given
import com.wbillingsley.handy.{LazyId, Ref, RefOpt, Refused}
import org.mindrot.jbcrypt.BCrypt

object UserDAO extends DAO(classOf[User], "assessoryUser", UserB.read) with datalayer.UserDAO {

  override def unsaved = User(id = UserId(allocateId))

  override def saveSafe(c:User) = {
    findAndReplace("_id" $eq c.id, UserB.write(c), upsert=true).toRef
  }

  /**
   * Saves the user's details
   */
  override def saveDetails(u:User) = updateAndFetch(
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
  override def saveNew(u:User) = saveSafe(u)

  /**
   * Adds an identity to this user
   */
  private def pushIdentity(ru:Ref[User], i:Identity):Ref[User] = {
    for {
      uid <- ru.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,
        update = $push("identities" -> IdentityB.write(i))
      ).require
    } yield u
  }

  /** Adds a session to this user. Typically this happens at login. */
  private def pushSession(ru:Ref[User], as:ActiveSession):Ref[User] = {
    for {
      uid <- ru.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,

        // TODO: we're just retaining 10 sessions due to LTI sessions profilerating. If we improve how LTI login works we can change this.
        // (issue with the session key not being passed to the app on the initial third party POST request due to SameSite restrictions).
        update = $pushLimit(-10, "activeSessions", ActiveSessionB.write(as))
      ).require
    } yield u
  }

  private def deleteSession(ru:Ref[User], as:ActiveSession):Ref[User] = {
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

  private def byIdentity(service:String, id:String):RefOpt[User] = {
    findOne(query=("identities.service" $eq service) and ("identities.value" $eq id))
  }

  override def byIdentity(i:Identity):RefOpt[User] = bySocialIdOrUsername(i.service, i.value, i.username)

  override def bySocialIdOrUsername(service:String, optId:Option[String], optUserName:Option[String] = None):RefOpt[User] = {

    def byId(service:String, oid:Option[String]) = for {
      id <- RefOpt(oid)
      u <- findOne(query=("identities.service" $eq service) and ("identities.value" $eq id))
    } yield u

    def byUsername(service:String, oun:Option[String]) = for {
      n <- RefOpt(oun)
      u <- findOne(query=("identities.service" $eq service) and ("identities.username" $eq n))
    } yield u

    byId(service, optId) orElse byUsername(service, optUserName)
  }

  private def byUsername(u:String) = findOne("pwlogin.username" $eq u)

  override def byEmail(e:String) = findOne("pwlogin.email" $eq e)

  override def byUsernameAndPassword(username:String, password:String) = {
    for (
      user <- byUsername(username) if checkPassword(user.pwlogin, password)
    ) yield user
  }

  override def byEmailAndPassword(email:String, password:String) = {
    for (
      user <- byEmail(email) if checkPassword(user.pwlogin, password)
    ) yield user
  }

  private def byCourse(c:Ref[Course]) = {
    c.refId map ("registrations.course" $eq _) flatMap findMany
  }

  override def addSession(user: Ref[User], session: ActiveSession): Ref[User] = pushSession(user, session)

  override def removeIdentity(user: Ref[User], identity: Identity): Ref[User] = {
    for {
      uid <- user.refId.require
      u <- updateAndFetch(
        query = "_id" $eq uid,
        update = $pull("identities" -> IdentityB.write(identity)) // TODO: deal with mismatch id/value
      ).require
    } yield u
  }

  override def removeSession(user: Ref[User], sessionKey: String): Ref[User] = deleteSession(user, ActiveSession(key=sessionKey, ip=""))

  override def addIdentity(user: Ref[User], identity: Identity): Ref[User] = pushIdentity(user, identity)


  /**
   * Generate a salt and hash for this password
   */
  override def hash(password: String) = BCrypt.hashpw(password, BCrypt.gensalt())

  /**
   * Check if the given password matches the given password login
   */
  override def checkPassword(pwlogin:PasswordLogin, pw:String) = pwlogin.pwhash match {
    case Some(hashed) => BCrypt.checkpw(pw, hashed)
    case None => false
  }
}
