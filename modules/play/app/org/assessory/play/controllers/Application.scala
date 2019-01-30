package org.assessory.play.controllers

import javax.inject.Inject

import com.assessory.asyncmongo.UserDAO
import play.api.mvc._
import util.UserAction

object Application {
  def getSession(request:RequestHeader):String = {
    request.session.get("sessionKey").getOrElse(java.util.UUID.randomUUID().toString)
  }

  def getUser(request:RequestHeader) = UserDAO.bySessionKey(getSession(request))
}

class Application @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction, indexTemplate: views.html.index)
  extends AbstractController(cc) {

  /**
    * Another debugging method, this time returning the name of the logged in user
    */
  def whoAmI:Action[AnyContent] = Action.async { request =>
    (for {
      u <- Application.getUser(request)
    } yield Ok(u.name.toString)).require.toFuture
  }

  /**
    * Just to test if anything is working, it's sometimes useful to have a very simple ok action
    */
  def default = Action {
    Ok("Default")
  }

  /**
    * This accepts an LTI launch post and returns the details it contained
    */
  def ltiTest = Action { request =>

    def getParam(params:Map[String, Seq[String]], name:String) = params.get(name).flatMap(_.headOption)

    val r = for {
      body <- request.body.asFormUrlEncoded
    } yield body.toString()

    Ok(r.toString)
  }

  /**
    * The HTML and Javascript for the client side of the app.
    * This also ensures the user's Play session cookie includes
    * a value for sessionKey.
    */
  def index = userAction { r =>
    Ok(indexTemplate())
  }

}
