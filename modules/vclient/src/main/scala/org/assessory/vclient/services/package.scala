package org.assessory.vclient

import com.wbillingsley.handy.Refused
import com.assessory.api.appbase.UserError
import org.scalajs.dom
import org.scalajs.dom.ext.AjaxException

import scala.concurrent.{ExecutionContext, Future}

package object services {

  val AJAX_HEADERS =  Map("Accept" -> "application/json", "Content-Type" -> "text/plain; charset=utf-8")

  implicit class FXHROps(val fxhr:Future[dom.XMLHttpRequest]) extends AnyVal {
    def responseText(implicit ec:ExecutionContext):Future[String] = fxhr.map(_.responseText) recoverWith {
      case AjaxException(req) if req.status == 403 => Future.failed[String](Refused(req.responseText))
    }
  }

  implicit class FutOps[T](val f:Future[T]) extends AnyVal {
    def optional404(implicit ec:ExecutionContext) = f.map(Some(_)) recover { case AjaxException(req) if req.status == 404 => None }

    def captureUserError(implicit ec:ExecutionContext):Future[T] = f recoverWith {
      case AjaxException(req) if req.status == 400 => Future.failed(UserError(req.responseText))
    }

    def captureError(implicit ec:ExecutionContext):Future[T] = f recoverWith {
      case AjaxException(req) if req.status == 400 => Future.failed(UserError(req.responseText))
      case AjaxException(req) if req.status == 403 => Future.failed(Refused(req.responseText))
      case AjaxException(req) => Future.failed(new RuntimeException(req.responseText))
    }
  }


}
