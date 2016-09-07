package org.assessory.play.oauth

import com.google.inject.Inject
import com.wbillingsley.handy.{RefFuture, Ref}
import Ref._
import play.api.{Logger, Configuration}
import play.api.mvc.{Results, Result, Action, Controller}
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
class GitHubController @Inject() (playAuth:PlayAuth, config:Configuration, ws:WSClient) extends Controller {

  val clientKey = config.getString("auth.github.ckey")
  val secret = config.getString("auth.github.csecret")

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
  def requestAuth = Action { implicit request =>
    val randomString = java.util.UUID.randomUUID().toString()
    val returnUrl = ""

    if (GitHub.available(config)) {
      Redirect(
        "https://github.com/login/oauth/authorize",
        Map(
          "state" -> Seq(randomString),
          "client_id" -> clientKey.toSeq
        ),
        303
      ).withSession(request.session + ("oauth_state" -> randomString))
    } else {
      InternalServerError("This server's client key and secret for GitHub have not been set")
    }
  }

  /**
    * Calls GitHub to swap a code for an auth_token
    */
  private def authTokenFromCode(code:String):Ref[String] = {
    val wsr = ws.url("https://github.com/login/oauth/access_token").
      withHeaders("Accept" -> "application/json").
      post(Map(
        "code" -> Seq(code),
        "client_id" -> clientKey.toSeq,
        "client_secret" -> secret.toSeq
      ))
    val authToken = for {
      resp <- wsr.toRef
      tok <- {
        println(resp.json); (resp.json \ "access_token").asOpt[String]
      }
    } yield tok
    authToken
  }

  /**
    * Given an authentication token, goes and looks up that user's details on GitHub.
    * These are filled into an "Interstitial Memory" -- details to remember during the display
    * of the confirmation page.
    */
  private def userFromAuth(authToken:String):Ref[OAuthDetails] = {
    val wsr = ws.url("https://api.github.com/user").
      withHeaders(
        "Accept" -> "application/json",
        "Authorization" -> ("token " + authToken)
      ).get()

    for {
      resp <- wsr.toRef
      json = resp.json
      id <- (resp.json \ "id").asOpt[Int].map(_.toString)
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

  def callback = UserAction.async { implicit request =>

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
      code <- Ref(request.getQueryString("code")) orIfNone AuthFailed("GitHub provided no code")
      authToken <- authTokenFromCode(code) orIfNone AuthFailed("GitHub did not provide an authorization token")
      mem <- userFromAuth(authToken) orIfNone AuthFailed("GitHub did not provide any user data for that login")
    } yield mem

    playAuth.onAuthR(request, rMem).toFuture
  }

}