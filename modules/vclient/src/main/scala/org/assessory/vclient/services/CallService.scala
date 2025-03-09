package org.assessory.vclient.services

import com.assessory.api.appbase.UserError
import com.assessory.api.call.{Call, Return, ReturnSession, UserCall}
import com.assessory.clientpickle.{CallClient, CallPickles}
import com.wbillingsley.handy.{Approval, Id, Latch, Ref, RefMany, Refused, refOps}
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalajs.dom.RequestInit
import org.scalajs.dom.HttpMethod
import org.scalajs.dom.Headers

import scala.scalajs.js.Thenable.Implicits.thenable2future

val callEndpoint = "/api/call"
val CALL_AJAX_HEADERS = Map("Accept" -> "application/json", "Content-Type" -> "application/json")

def makeNetworkCall(call:Call):Future[Return] = {

  for {
    httpResponse <- dom.fetch(callEndpoint, new RequestInit {
      method = HttpMethod.POST      
      headers = scalajs.js.Dictionary(
          "Accept" -> "application/json",
          "Content-Type" -> "application/json",
        )    
      body = CallPickles.write(call)
    })
    text <- {
      httpResponse.status match {
        case 200 => httpResponse.text():Future[String]
        case 400 => Future.failed(UserError(httpResponse.statusText)) // Bad request
        case 403 => Future.failed(Refused(httpResponse.statusText)) // Forbidden
        case 404 => Future.failed(new NoSuchElementException("Not found")) // NotFound
        case 500 => Future.failed(RuntimeException(httpResponse.statusText)) // Internal Server Error
        case x => Future.failed(IllegalArgumentException(s"Could not match response code $x. ${httpResponse.statusText}"))
      }    
    }
    resp <- CallPickles.readReturnF(text)
  } yield resp
}

val callClient = CallClient(using makeNetworkCall)