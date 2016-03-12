package controllers

import play.api.mvc.{Action, Controller}

class Application extends Controller {

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
