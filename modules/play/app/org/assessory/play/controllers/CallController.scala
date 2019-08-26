package org.assessory.play.controllers

import com.assessory.api.call.{GetSession, ReturnSession}
import com.assessory.clientpickle.CallPickles
import com.assessory.model.CallsModel
import com.wbillingsley.handy.appbase.{ActiveSession, UserError}
import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents, Results}
import util.UserAction
import com.wbillingsley.handy.Ref._

class CallController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)
  extends AbstractController(cc) {


  def call = userAction.async { implicit request =>

    val wp = for {
      text <- request.body.asText.toRef orFail UserError("There was no call body to parse")
      call <- CallPickles.readCallR(text)
      result <- call match {
        case GetSession => ReturnSession(ActiveSession(request.sessionKey, request.remoteAddress, System.currentTimeMillis())).itself
        case _ => CallsModel.call(request.approval, call)
      }
    } yield Results.Ok(CallPickles.write(result)).as("application/json")

    wp.toFuture
  }

}
