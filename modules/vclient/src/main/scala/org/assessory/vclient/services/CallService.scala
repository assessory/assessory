package org.assessory.vclient.services

import com.assessory.api.call.{Call, Return, ReturnSession, UserCall}
import com.assessory.clientpickle.{CallPickles, CallClient}
import com.wbillingsley.handy.{Approval, Id, Latch, Ref, RefMany, refOps}
import org.scalajs.dom.ext.Ajax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


val callEndpoint = "/api/call"
val CALL_AJAX_HEADERS = Map("Accept" -> "application/json", "Content-Type" -> "application/json")

def makeNetworkCall(call:Call):Future[Return] = {
  for {
    req <- Ajax.post(callEndpoint, CallPickles.write(call), headers=CALL_AJAX_HEADERS)
    resp <- CallPickles.readReturnF(req.responseText)
  } yield resp
}

val callClient = CallClient(using makeNetworkCall)