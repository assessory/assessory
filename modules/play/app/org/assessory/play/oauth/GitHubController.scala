package org.assessory.play.oauth

import com.wbillingsley.handy.{Ref, RefFuture, RefOpt}
import Ref._
import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits._
import util.UserAction

case object GitHub extends Service {
  val name = "GitHub"
  def available(config:Configuration) = {
    true //config.getString("auth.github.ckey").isDefined && config.getString("auth.github.csecret").isDefined
  }
}

/**
  * Controller for GitHub OAuth. Based on handy-play-oauth, updated for Play 2.5
  */
class GitHubController @Inject() (cc: ControllerComponents, playAuth:PlayAuth, config:Configuration, ws:WSClient, userAction: UserAction)
  extends AbstractController(cc) {

  val clientKey:Option[String] = config.getOptional[String]("auth.github.ckey")
  val secret:Option[String] = config.getOptional[String]("auth.github.csecret")

  def getAuth = if (playAuth.allowGet) {
    requestAuth
  } else {
    Action { Results.Forbidden("OAuth may not be initiated via a GET request on this server") }
  }

  /**
    * Beginning of the Sign in with GitHub flow, using OAuth2.
    * A random state is set in the session, and then the user is redirected to the GitHub
    * sign-in endpoint.
    */
  def requestAuth = Action { request =>
    val randomString = java.util.UUID.randomUUID().toString()
    val returnUrl = ""

    if (GitHub.available(config)) {
      Redirect(
        url="https://github.com/login/oauth/authorize",
        queryString=Map(
          "state" -> Seq(randomString),
          "client_id" -> clientKey.toSeq
        ),
        status=303
      ).withSession(request.session + ("oauth_state" -> randomString))
    } else {
      InternalServerError("This server's client key and secret for GitHub have not been set")
    }
  }

  /**
    * Calls GitHub to swap a code for an auth_token
    */
  private def authTokenFromCode(code:String):RefOpt[String] = {
    val wsr = ws.url("https://github.com/login/oauth/access_token").
      withHttpHeaders("Accept" -> "application/json").
      post(Map(
        "code" -> Seq(code),
        "client_id" -> clientKey.toSeq,
        "client_secret" -> secret.toSeq
      ))
    val authToken = for {
      resp <- wsr.toRef
      tok <- (resp.json \ "access_token").asOpt[String].toRef
    } yield tok
    authToken
  }

  /**
    * Given an authentication token, goes and looks up that user's details on GitHub.
    * These are filled into an "Interstitial Memory" -- details to remember during the display
    * of the confirmation page.
    */
  private def userFromAuth(authToken:String):RefOpt[OAuthDetails] = {
    val wsr = ws.url("https://api.github.com/user").
      withHttpHeaders(
        "Accept" -> "application/json",
        "Authorization" -> ("token " + authToken)
      ).get()

    for {
      resp <- wsr.toRef
      json = resp.json
      id <- (resp.json \ "id").asOpt[Int].map(_.toString).toRef
    } yield {
      OAuthDetails(
        userRecord = UserRecord(
          service = "GitHub",
          id = id,
          name = (json \ "name").asOpt[String],
          nickname = (json \ "login").asOpt[String],
          username = (json \ "login").asOpt[String],
          avatar = (json \ "avatar_url").asOpt[String]
        ),
        raw = Some(json)
      )
    }
  }

  def callback = userAction.async { implicit request =>

    val stateFromSession = request.session.get("oauth_state")
    val stateFromRequest = request.getQueryString("state")

    /*
     * TODO: We've had a few errors where we were getting a mismatch between the OAuth state in the
     * session and in the callback from GitHub. For the moment, let's turn off the check and log
     * whenever there is a mismatch to see if we can uncover why.
     */
    if (stateFromSession.isEmpty) {
      Logger.warn("GitHub OAuth - state from session was empty")
    }
    if (stateFromRequest.isEmpty) {
      Logger.warn("GitHub OAuth - state from request was empty")
    }
    if (stateFromSession != stateFromRequest) {
      Logger.warn(s"GitHub OAuth - state from request was $stateFromRequest but state from session was $stateFromSession")
    }

    val rMem = for {
      code <- request.getQueryString("code").toRef orFail AuthFailed("GitHub provided no code")
      authToken <- authTokenFromCode(code) orFail AuthFailed("GitHub did not provide an authorization token")
      mem <- userFromAuth(authToken) orFail AuthFailed("GitHub did not provide any user data for that login")
    } yield mem

    playAuth.onAuthR(request, rMem).toFuture
  }

}