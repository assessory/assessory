package com.assessory.clientpickle

import com.assessory.api.appbase.{ActiveSession, User}
import com.wbillingsley.handy.Latch
import com.assessory.api.call.{Call, Return, ReturnSession, ReturnUser, SessionCall}

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 * @param networkService - the actual function that communicates over the wire
 */
class CallClient(using networkService: Call => Future[Return], ec:ExecutionContext) {

  /**
   * The session is kept private to avoid the key becoming accessible to other javascript (session hijacking)
   */
  private val session:Latch[ActiveSession] = Latch.lazily(
    for ReturnSession(as) <- networkService(SessionCall.GetSession) yield as
  )

  def makeCall(call:Call):Future[Return] = session.request.flatMap(s => networkService(SessionCall.WithSession(s, call)))

  def login(email:String, password:String):Future[User] = {
    for
      s <- session.request
      ReturnUser(u) <- makeCall(SessionCall.Login(email, password, s))
    yield u
  }

  def logout():Future[User] = {
    for
      s <- session.request
      _ <- makeCall(SessionCall.Logout(s))
    do 
      
  }

  def register(email:String, password:String):Future[User] = {
    for
      s <- session.request
      ReturnUser(u) <- makeCall(SessionCall.Login(email, password, s))
    yield u
  }

}
