package org.assessory.play.cheatscript

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.*
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, FromResponseUnmarshaller, Unmarshal, Unmarshaller}
import com.assessory.api.call.{Call, Return, ReturnSession, UserCall}
import com.wbillingsley.handy.{Ref, Refused, refOps}
import com.assessory.api.appbase.{ActiveSession, UserError}
import com.assessory.clientpickle.CallPickles
import com.assessory.clientpickle.CallPickles.*

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.*
import akka.http.scaladsl.client.RequestBuilding.Post


class NetworkService(url:String) {

  given FromResponseUnmarshaller[Return] = Unmarshaller.messageUnmarshallerFromEntityUnmarshaller(
    Unmarshaller.stringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .flatMap(
        ctx => mat => json => CallPickles.readReturnF(json)
      )
  )

  given ToEntityMarshaller[Call] =
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { a =>
      HttpEntity(MediaTypes.`application/json`, CallPickles.write(a))
    }

  given system:ActorSystem[Behaviors.type] = ActorSystem(Behaviors.empty, "NetworkClientActor")
  system.whenTerminated.foreach { _ =>
    System.exit(0)
  }

  given ec:ExecutionContext = system.executionContext

  def close() = {
    system.terminate()
  }

  given networkCall:(Call => Future[Return]) = { call =>

    println(s"Making call $call")
    for {
      resp <- Http().singleRequest(Post(url, call))

      _ = println(s"Status ${resp.status}")

      data <- resp match {
        case HttpResponse(StatusCodes.OK, headers, entity, _) =>
          Unmarshal(resp).to[Return]
        case HttpResponse(StatusCodes.NotFound, _, _, _) =>
          Future.failed(new NoSuchElementException)
        case HttpResponse(StatusCodes.Forbidden, _, entity, _) =>
          Unmarshal(entity).to[String].flatMap(err => Future.failed(Refused(err)))
        case HttpResponse(StatusCodes.BadRequest, _, entity, _) =>
          Unmarshal(entity).to[String].flatMap(err => Future.failed(UserError(err)))
        case HttpResponse(_, _, entity, _) =>
          Unmarshal(entity).to[String].flatMap(err => Future.failed(IllegalStateException(err)))
      }
    } yield {
      println(s"Returned $data")
      data
    }
  }

}

