package controllers

import com.assessory.asyncmongo.UserDAO
import play.api.mvc.{RequestHeader, Request, Action, Controller}


object Application {
  def getSession(request:RequestHeader):String = {
    request.session.get("sessionKey").getOrElse(java.util.UUID.randomUUID().toString)
  }

  def getUser(request:RequestHeader) = UserDAO.bySessionKey(getSession(request))
}

class Application extends Controller {

  def whoAmI = Action.async { request =>
    (for {
      u <- Application.getUser(request)
    } yield Ok(u.name.toString)).toFuture
  }

  def default = Action {
    Ok("Default")
  }

  def ltiTest = Action { request =>

    def getParam(params:Map[String, Seq[String]], name:String) = params.get(name).flatMap(_.headOption)

    val r = for {
      body <- request.body.asFormUrlEncoded
    } yield body.toString()

    Ok(r.toString)
  }

  def ltiRegTest = {

  }

}
