package org.assessory.akkahttp

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.StatusCodes.{Forbidden, InternalServerError, NotFound}
import akka.http.scaladsl.model.headers.{HttpCookie, SameSite}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import com.assessory.api.appbase.{ActiveSession, Course, CourseId, CourseRole, Identity, User, UserId}

import scala.io.StdIn
import com.wbillingsley.handy.{Approval, EmptyKind, RefFuture, RefFailed, Refused, refOps, lazily}
import com.assessory.api.call.{Call, Return, ReturnSession, SessionCall}
import com.assessory.asyncmongo.{DB, RegistrationDAO, UserDAO}
import com.assessory.clientpickle.CallPickles
import com.assessory.model.{CallsModel, DoWiring, UserModel}
import com.assessory.api.wiring.Lookups.{given, _}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

private val random = new java.security.SecureRandom()
private def randomSessionId() = {
  new java.math.BigInteger(120, random).toString(32)
}
private def randomSessionCookie() = HttpCookie.apply(
  name="assessorySession",
  value=randomSessionId(),
  path=Some("/")
).withSameSite(SameSite.Lax)

given FromRequestUnmarshaller[Call] = Unmarshaller.messageUnmarshallerFromEntityUnmarshaller(
    Unmarshaller.stringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .flatMap(
        ctx => mat => json => CallPickles.readCallF(json)
      )
  )

given ToEntityMarshaller[Return] =
  Marshaller.withFixedContentType(MediaTypes.`application/json`) { a =>
    HttpEntity(MediaTypes.`application/json`, CallPickles.write(a))
  }

given ExceptionHandler = ExceptionHandler {
  case _:NoSuchElementException =>
    complete(HttpResponse(NotFound))
  case Refused(msg) =>
    complete(HttpResponse(Forbidden, entity=msg))
  case NonFatal(e) =>
    complete(HttpResponse(InternalServerError, entity =
      s"""ERROR: ${e.getMessage}
         |
         |${e.getStackTrace.map(_.toString).mkString("\n")}
         |""".stripMargin))
}



@main def startServer() = {
  given system:ActorSystem[Any] = ActorSystem(Behaviors.empty, "assessory-system")
  given ec:ExecutionContext = system.executionContext

  /* Start-up config */

  // TODO: DB.dbName = "assessory_2019_1"
  // Set the execution context (ie the thread pool) that RefFuture work should happen on
  RefFuture.executionContext = ec
  // Wire up the lookups
  DoWiring.doWiring

  val route = Route.seal(concat(
    path("ping") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "pong"))
      }
    },

    // Force the session cookie to be set on load of index.html
    pathSingleSlash {
      get {
        optionalCookie("assessorySession") {
          case Some(sessionCookie) =>
            encodeResponse {
              getFromResource("static/index.html")
            }

          case None =>
            setCookie(randomSessionCookie()) {
              encodeResponse {
                getFromResource("static/index.html")
              }
            }
        }
      }
    },

    path("api" / "call") {
      post {
        decodeRequest {
          extractClientIP { ip =>
            entity(as[Call]) { call =>
              optionalCookie("assessorySession") {
                case Some(sessionCookie) =>
                  complete {
                    call match {
                      case SessionCall.GetSession => ReturnSession(ActiveSession(sessionCookie.value, ip.value))
                      case _ => CallsModel.call(Approval(UserDAO.bySessionKey(sessionCookie.value)), call).toFuture
                    }
                  }

                case None =>
                  val cookie = randomSessionCookie()
                  val session = ActiveSession(cookie.value(), ip.value)

                  setCookie(cookie) {
                    complete {
                      call match {
                        case SessionCall.GetSession => ReturnSession(session)
                        case _ => CallsModel.call(Approval(UserDAO.bySessionKey(session.key)), call).toFuture
                      }
                    }
                  }
              }

            }

          }

        }
      }
    },

    pathPrefix("lti1.1" / "course" / Segment / Remaining) { (courseId, remaining) =>
      post {

        println("LTI post")

        extractUri { uri =>

          println(uri)

          formFieldMap { fieldMap =>
            extractClientIP { ip =>

              val consumerKey = fieldMap.getOrElse("oauth_consumer_key", "")
              val email = fieldMap.getOrElse("lis_person_contact_email_primary", "")
              val name = fieldMap.getOrElse("lis_person_name_full", "")
              val roles = fieldMap.getOrElse("roles", "")

              val path = uri.path.toString()
              val authority = uri.authority.host.toString()
              val port = uri.authority.port.toString()
              val scheme = uri.scheme
              val params = fieldMap.toSeq

              val signature = fieldMap.getOrElse("oauth_signature", "")

              val redir = HttpResponse(
                status = StatusCodes.SeeOther,
                headers = Seq(headers.Location("/#/" + remaining)),
                entity = HttpEntity.Empty
              )

              optionalCookie("assessorySession") {
                case Some(sessionCookie) =>
                  println(s"Logging you in with existing session cookie ${sessionCookie.value}")
                  complete {
                    for
                      reg <- UserModel.lti11Login(CourseId(courseId), consumerKey, sessionCookie.value, ip.value, email, name, roles).toFuture
                    yield redir
                  }

                case None =>
                  val cookie = randomSessionCookie()
                  setCookie(cookie) {
                    complete {
                      (for
                        course <- CourseId(courseId).lazily
                        secret <- course.ltis.find(_.clientKey == consumerKey).map(_.secret).toRefOpt orFail Refused("Consumer key not found")

                        generatedSignature = Lti11Verifier.signature(
                          method="POST", scheme=scheme, authority=authority, port=port, path=path, parameters=params, secret
                        )
                        _ <- if signature == generatedSignature then true.itself else RefFailed(Refused(s"Generated signature $generatedSignature did not match request signature $signature"))

                        reg <- UserModel.lti11Login(CourseId(courseId), consumerKey, cookie.value(), ip.value, email, name, roles)
                      yield redir).toFuture
                    }
                  }
              }
            }
          }
        }
      }
    },

    pathPrefix("assets" / Remaining) { file =>
      // optionally compresses the response with Gzip or Deflate
      // if the client accepts compressed responses
      encodeResponse {
        getFromResource(file)
      }
    },

    pathPrefix("static" / Remaining) { file =>
      // optionally compresses the response with Gzip or Deflate
      // if the client accepts compressed responses
      encodeResponse {
        getFromResource("static/" + file)
      }
    },

    pathPrefix("assets" / Remaining) { file =>
      // optionally compresses the response with Gzip or Deflate
      // if the client accepts compressed responses
      encodeResponse {
        getFromResource("public/" + file)
      }
    },

    path("newcookie") {
      get {
        val cookie = randomSessionCookie()
        setCookie(cookie) {
          complete {
            s"New cookie is ${cookie.value()}"
          }
        }
      }
    }

  ))

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}