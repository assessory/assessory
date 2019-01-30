package util

import com.assessory.asyncmongo.UserDAO
import com.wbillingsley.handy.appbase.{User, UserError}
import com.wbillingsley.handy.{Approval, Ref, RefOpt, RefSome}
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class AppbaseRequest[A](request:Request[A]) extends WrappedRequest(request) {

  lazy val user:RefOpt[User] = UserDAO.user(request)

  lazy val approval:Approval[User] = new Approval(user)

  val sessionKey:String = request.session.get("sessionKey").getOrElse(AppbaseRequest.newSessionKey)
}

object AppbaseRequest {
  def newSessionKey:String = java.util.UUID.randomUUID.toString
}


class UserAction @Inject() (val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[AppbaseRequest, AnyContent] with ActionTransformer[Request, AppbaseRequest] {

  val errorPF:PartialFunction[Throwable, Future[Result]] = {
    case x:NoSuchElementException => Future.successful(Results.NotFound(x.getMessage))
    case UserError(x) => Future.successful(Results.BadRequest(x))
  }

  def transform[A](request: Request[A]) = Future.successful {
    new AppbaseRequest(request)
  }

  def invokeBlock[A](request: Request[A])(block:AppbaseRequest[A] => Future[Result]) = {
    (for {
      r <- transform(request)
      res <- block(r)
    } yield {
      res
        .withHeaders(
          "Cache-Control" -> "no-cache, no-store, must-revalidate", "Expires" -> "0", "Pragma" -> ""
        )
        .addingToSession("sessionKey" -> r.sessionKey)(r)
    }).recoverWith(errorPF)

  }

  implicit def rtf(rr:Ref[Result]):Future[Result] = {
    rr
      .toFuture
      .recoverWith(errorPF)
  }

  implicit def rotf(rr:RefOpt[Result]):Future[Result] = {
    rr
      .orElse(RefSome(Results.NotFound(Json.obj("error" -> "Not found"))))
      .require
      .toFuture
      .recoverWith(errorPF)
  }

}

