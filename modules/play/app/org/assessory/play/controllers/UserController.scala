package org.assessory.play.controllers

import com.assessory.api.client.EmailAndPassword
import com.assessory.api.wiring.Lookups._
import com.assessory.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{ActiveSession, User, UserError}
import com.wbillingsley.handyplay.{DataAction, WithHeaderInfo}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller, Result, Results}
import util.{RefConversions, UserAction}

import scala.concurrent.Future
import scala.language.implicitConversions
import RefConversions._
import Id._

class UserController extends Controller {

  implicit def userToResult(rc:Ref[User]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def manyUserToResult(rc:RefMany[User]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  /**
    *
    */
  def self = UserAction.async { implicit request =>
    request.approval.who
  }

  /**
   * Creates a user and logs them in
   */
  def signUp = UserAction.async { implicit request =>
    userToResult(
      for {
        text <- request.body.asText.toRef orIfNone RefFailed(UserError("You must supply an email and password"))
        ep = upickle.default.read[EmailAndPassword](text)
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
  def logIn = UserAction.async { implicit request =>
    userToResult(
      for {
        text <- request.body.asText.toRef orIfNone UserError("You must supply an email and password")
        ep = upickle.default.read[EmailAndPassword](text)
        u <- UserModel.logIn(
          oEmail = Some(ep.email),
          oPassword = Some(ep.password),
          session = ActiveSession(request.sessionKey, ip=request.remoteAddress)
        )
      } yield u
    )
  }

  /**
    * Handler for the "secret log-in link"
    */
  def autologin(userId:String, secret:String) = UserAction.async { implicit request =>
    val loggedIn = for {
      u <- UserModel.secretLogIn(
        ru = userId.asId[User].lazily,
        secret = secret,
        activeSession = ActiveSession(request.sessionKey, request.remoteAddress)
      )
    } yield Redirect(routes.Application.index())

    loggedIn.toFuture
  }

  /**
   * To log a user out, we just have to remove the current session from their active sessions
   */
  def logOut = UserAction.async { implicit request =>
    (for {
      u <- UserModel.logOut(
        rUser = request.user,
        session = ActiveSession(request.sessionKey, ip = request.remoteAddress)
      )
    } yield Results.Ok).toFuture
  }

  /**
    * Retrieves and returns many users
    */
  def findMany = UserAction.async { implicit request =>
    manyUserToResult(
      for {
        text <- request.body.asText.toRef
        ids = upickle.default.read[Ids[User,String]](text)
        wp <- UserModel.findMany(request.approval, ids)
      } yield wp
    )
  }

}
