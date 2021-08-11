package org.assessory.vclient.services

import com.assessory.api.call.{Call, Return, ReturnSession, UserCall}
import com.assessory.clientpickle.CallPickles
import com.wbillingsley.handy.{Approval, Id, Latch, Ref, RefMany, refOps}
import org.scalajs.dom.ext.Ajax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
 * Utilities for the Circe-based call API
 */
object CallService {

  private val callEndpoint = "/api/call"

  private val CALL_AJAX_HEADERS =  Map("Accept" -> "application/json", "Content-Type" -> "application/json")


  /** Makes a call to the server, returning the Return */
  def makeCall(call:Call):Future[Return] = {
    for {
      req <- Ajax.post(callEndpoint, CallPickles.write(call), headers=CALL_AJAX_HEADERS)
      resp <- CallPickles.readReturnF(req.responseText)
    } yield resp
  }

  def optCall(call:Call):Future[Option[Return]] = makeCall(call).optional404

  /* Performs a call and checks the particular subtype of return *
  inline def typedCall[R](call:Call):Future[R] = {
     makeCall(call).collect {
       case r:R => r
     }
  }*/

}
