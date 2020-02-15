package org.assessory.play.oauth

import javax.inject.{Inject, Singleton}
import com.assessory.asyncmongo.UserDAO
import com.wbillingsley.handy.appbase.ActiveSession
import com.wbillingsley.handy.{Ref, RefFuture, Refused}
import Ref._
import util.AppbaseRequest
import play.api.mvc._

import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Promise}
import play.api.{Configuration, Environment}

trait Service {
  val name:String
  def available(config:Configuration):Boolean
}

@Singleton
class PlayAuth @Inject() (environment:Environment, config:Configuration)(implicit ec:ExecutionContext) {

  /**
    * What the module should do on completion of an OAuth sign in.
    * By default, this is wired up (asynchronously) to <code>onAuth</code>.
    * (The version that passes a <code>Try</code> rather than a <code>Ref</code>)
    */
  def onAuthR(request: AppbaseRequest[AnyContent], rm:Ref[OAuthDetails]):Ref[Result] = {
    (for {
      mem <- rm
      u <- UserDAO.bySocialIdOrUsername(mem.userRecord.service, Some(mem.userRecord.id), mem.userRecord.username) orFail Refused(s"I couldn't find any users with ${mem.userRecord.service} ID ${mem.userRecord.username.getOrElse("")}. Check for typos / wrong case")
      withSession <- UserDAO.pushSession(u.itself, ActiveSession(request.sessionKey, ip = request.remoteAddress))

      saved <- {
        // Update any missing user details
        val updated = withSession.copy(
          name = u.name orElse mem.userRecord.name,
          nickname = u.nickname orElse mem.userRecord.nickname,
          avatar = u.avatar orElse mem.userRecord.avatar
        )

        if (updated == withSession) {
          withSession.itself
        } else {
          UserDAO.saveDetails(updated).require
        }
      }
    } yield Results.Redirect(org.assessory.play.controllers.routes.Application.index())) recoverWith { case x => Results.Forbidden(x.getMessage).itself }
  }

  var allowGet = config.getOptional[Boolean]("auth.oauth.allowGet").getOrElse(false)

  val allServices = Seq(
    GitHub
  )

  def enabledServices = allServices.filter(_.available(config))

}



