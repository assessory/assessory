package org.assessory.play.controllers

import com.assessory.api.client.EmailAndPassword
import com.assessory.api.wiring.Lookups._
import com.assessory.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{ActiveSession, User, UserError}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import util.{RefConversions, UserAction}

import scala.concurrent.Future
import scala.language.implicitConversions
import RefConversions._
import Id._
import com.assessory.clientpickle.Pickles
import Pickles._
import javax.inject.Inject

class UserController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)
  extends AbstractController(cc) {

  implicit def userToResult(rc:Ref[User]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def manyUserToResult(rc:RefMany[User]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  /**
    *
    */
  def self = userAction.async { implicit request =>
    (for {
      u <- request.approval.who.require
      r <- userToResult(u.itself).toRef
    } yield r).toFuture
  }

  /**
   * Creates a user and logs them in
   */
  def signUp = userAction.async { implicit request =>
    userToResult(
      for {
        text <- request.body.asText.toRef orFail UserError("You must supply an email and password")
        ep <- Pickles.read[EmailAndPassword](text).toRef
        u <- UserModel.signUp(
          oEmail = Some(ep.email),
          oPassword = Some(ep.password),
          session = ActiveSession(request.sessionKey, ip=request.remoteAddress)
        )
      } yield u
    )
  }

  /**
   * Logging a user in involves finding the user (if the password hash matches), and pushing the
   * current session key as an active session
   */
  def logIn = userAction.async { implicit request =>
    userToResult(
      for {
        text <- request.body.asText.toRef orFail UserError("You must supply an email and password")
        ep <- Pickles.read[EmailAndPassword](text).toRef
        u <- UserModel.logIn(
          oEmail = Some(ep.email),
          oPassword = Some(ep.password),
          session = ActiveSession(request.sessionKey, ip=request.remoteAddress)
        ) orFail Refused("Incorrect login or password")
      } yield u
    )
  }

  /**
    * Handler for the "secret log-in link"
    */
  def autologin(userId:String, secret:String) = userAction.async { implicit request =>
    val loggedIn = for {
      u <- UserModel.secretLogIn(
        ru = userId.asId[User].lazily,
        secret = secret,
        activeSession = ActiveSession(request.sessionKey, request.remoteAddress)
      ) orFail Refused("Incorrect autologin")
    } yield Redirect(routes.Application.index())

    loggedIn.toFuture
  }

  /**
   * To log a user out, we just have to remove the current session from their active sessions
   */
  def logOut = userAction.async { implicit request =>
    (for {
      u <- UserModel.logOut(
        rUser = request.user.require,
        session = ActiveSession(request.sessionKey, ip = request.remoteAddress)
      )
    } yield Results.Ok).toFuture
  }

  /**
    * Retrieves and returns many users
    */
  def findMany = userAction.async { implicit request =>
    manyUserToResult(
      for {
        text <- request.body.asText.toRef
        ids <- Pickles.read[Ids[User,String]](text).toRef
        wp <- UserModel.findMany(request.approval, ids)
      } yield wp
    )
  }

  /**
    * Retrieves and returns many users
    */
  def findOne(id:String) = userAction.async { implicit request =>
    userToResult(
      id.asId[User].lazily
    )
  }

}
