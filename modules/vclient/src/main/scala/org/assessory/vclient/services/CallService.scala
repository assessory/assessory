package org.assessory.vclient.services

import com.assessory.api.appbase.UserError
import com.assessory.api.call.{Call, Return, ReturnSession, UserCall}
import com.assessory.clientpickle.{CallClient, CallPickles}
import com.wbillingsley.handy.{Approval, Id, Latch, Ref, RefMany, Refused, refOps}
import org.scalajs.dom.ext.{Ajax, AjaxException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


val callEndpoint = "/api/call"
val CALL_AJAX_HEADERS = Map("Accept" -> "application/json", "Content-Type" -> "application/json")

def makeNetworkCall(call:Call):Future[Return] = {
  (for {
    req <- Ajax.post(callEndpoint, CallPickles.write(call), headers=CALL_AJAX_HEADERS)
    resp <- CallPickles.readReturnF(req.responseText)
  } yield resp).recoverWith { case AjaxException(req) =>
    req.status match {
      case 400 => Future.failed(UserError(req.responseText)) // Bad request
      case 403 => Future.failed(Refused(req.responseText)) // Forbidden
      case 404 => Future.failed(new NoSuchElementException("Not found")) // NotFound
      case 500 => Future.failed(RuntimeException(req.responseText)) // Internal Server Error
      case _ => Future.failed(IllegalArgumentException(s"Could not match response code ${req.status}. ${req.responseText}"))
    }
  }
}

val callClient = CallClient(using makeNetworkCall)