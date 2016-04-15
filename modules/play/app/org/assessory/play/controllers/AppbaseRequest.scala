package org.assessory.play.controllers

import com.assessory.asyncmongo.UserDAO
import com.wbillingsley.handy.Approval
import play.api.mvc.{Request, WrappedRequest}


class AppbaseRequest[A, U](request:Request[A]) extends WrappedRequest(request) {

  lazy val user = UserDAO.user(request)

  lazy val approval = new Approval(user)

  val sessionKey = request.session.get("sessionKey").getOrElse(AppbaseRequest.newSessionKey)
}

object AppbaseRequest {

  def newSessionKey = java.util.UUID.randomUUID.toString

}

