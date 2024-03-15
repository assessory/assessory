package com.assessory.model

import com.assessory.api.wiring.Lookups.{given, *}
import com.assessory.asyncmongo.*
import com.wbillingsley.handy.{Approval, EmptyKind, Id, Ref, Refused, lazily, refOps}
import com.assessory.api.appbase.{ActiveSession, Course, CourseId, CourseRole, Identity, User, UserError, UserId}

object UserModel {

  /**
   * Creates a user and logs them in
   */
  def signUp(oEmail:Option[String], oPassword:Option[String], session:ActiveSession) = {
    for (
      email <- oEmail.toRefOpt orFail UserError("Email must not be blank");
      password <- oPassword.toRefOpt orFail UserError("Password must not be blank");
      user <- {
        val u = UserDAO.unsaved
        val set = u.copy(
          pwlogin=u.pwlogin.copy(email=Some(email), pwhash=Some(UserDAO.hash(password))),
          activeSessions=Seq(session)
        )
        UserDAO.saveNew(set)
      }
    ) yield user
  }

  def byEmail(email:String) = UserDAO.byEmail(email)

  def byIdentity(id:Identity) = UserDAO.byIdentity(id)

  /**
   * Logging a user in involves finding the user (if the password hash matches), and pushing the
   * current session key as an active session
   */
  def logIn(oEmail:Option[String], oPassword:Option[String], session:ActiveSession) = {
    for {
      email <- oEmail.toRefOpt orFail UserError("Email must not be blank")
      password <- oPassword.toRefOpt orFail UserError("Password must not be blank")
      user <- UserDAO.byEmailAndPassword(email, password)
      updated <- UserDAO.addSession(user.itself, session)
    } yield updated
  }

  /**
   * Logs a user in using their system-set secret
   */
  def secretLogIn(ru:Ref[User], secret:String, activeSession:ActiveSession) = {
    for {
      oldUser <- UserDAO.removeSession(ru, activeSession.key).toRefOpt
      u <- ru if u.secret == secret
      pushed <- UserDAO.addSession(u.itself, activeSession)
    } yield pushed
  }

  /**
   * To log a user out, we just have to remove the current session from their active sessions
   */
  def logOut(rUser:Ref[User], session:ActiveSession) = {
    for (
      u <- rUser;
      user <- UserDAO.removeSession(u.itself, session.key)
    ) yield {
      user
    }
  }

  def findMany(a:Approval[User], ids:Seq[Id[User,String]]) = ids.lookUp

  def displayName(u:User):String = {
    u.name.orElse(u.pwlogin.email.orElse(u.identities.find(_.username.nonEmpty).flatMap(_.username))).getOrElse("Unnamed user")
  }

  /** Perform an LTI 1.1 login to a course */
  def lti11Login(courseId:CourseId, consumerKey:String, session:String, ip:String, email:String, name:String, roles:String):Ref[Course.Reg] = {
    val service = "LTI" + consumerKey
    def courseContainsLti(c:Course, ck:String) = {
      c.ltis.exists(_.clientKey == ck)
    }

    for
      prevUser <- {
        for
          u <- UserDAO.bySessionKey(session)
          out <- UserModel.logOut(u.itself, ActiveSession(session, ip=ip))
        yield out
      }.option

      course <- (for c <- courseId.lazily.toRefOpt if courseContainsLti(c, consumerKey) yield c) orFail Refused("Client key did not match")

      user <- {
        UserDAO.bySocialIdOrUsername(service=service, optId=Some(email), optUserName=Some(email)) orElse {
          UserDAO.saveNew(User(
            id = UserId(UserDAO.allocateId),
            name = Some(name),
            identities = Seq(Identity(service=service, value=Some(email), username=Some(email)))
          ))
        }
      }

      loggedIn <- UserDAO.addSession(user.itself, ActiveSession(key=session, ip=ip))

      reg <- RegistrationDAO.course.register(
        user.id, course.id,
        if roles.contains("Instructor") then Set (CourseRole.student, CourseRole.staff) else Set(CourseRole.student),
        EmptyKind
      )
    yield reg
  }


}
