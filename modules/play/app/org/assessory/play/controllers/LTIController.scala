package org.assessory.play.controllers

import java.io.{File, FileWriter}

import com.assessory.asyncmongo.{CourseDAO, RegistrationDAO, UserDAO}
import com.assessory.model.UserModel
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy.appbase._
import com.wbillingsley.handy.{EmptyKind, Refused}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, Controller}
import play.core.parsers.Multipart

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class LTIController extends Controller {


  def courseLtiAndRedirect(cid:String, path:String) = Action.async { request =>

    val session = Application.getSession(request)
    val sUser = UserDAO.bySessionKey(session)

    def getParam(params:Map[String, Seq[String]], name:String) = params.get(name).flatMap(_.headOption)

    def courseContainsLti(c:Course, ck:String) = {
      c.ltis.exists(_.clientKey == ck)
    }

    val rCourse = for {
      // Get the course if it's available and the details are right
      body <- request.body.asFormUrlEncoded.toRef orIfNone Refused("No LTI data in request")
      clientKey <- getParam(body, "oauth_consumer_key").toRef orIfNone Refused("No client key in request")
      course <- CourseDAO.lookUp.one(cid.asId).withFilter(courseContainsLti(_, clientKey)) orIfNone Refused("Client key did not match")

      username <- getParam(body, "lis_person_contact_email_primary").toRef orIfNone Refused("Couldn't find a user identity (email) in the request")
      service = (course.id.id, clientKey).toString

      // log any previous user out
      prevUser <- optionally {
        for {
          u <- UserModel.logOut(
            rUser = sUser,
            session = ActiveSession(session, ip=request.remoteAddress)
          )
        } yield u
      }

      // The user if there already is one
      user <- {
        UserDAO.bySocialIdOrUsername(service=service, optId=Some(username), optUserName=Some(username)) orIfNone {
          UserDAO.saveNew(User(
            id = UserDAO.allocateId.asId,
            name = getParam(body, "lis_person_name_full"),
            identities = Seq(Identity.apply(service=service, value=Some(username), username=Some(username)))
          ))
        }
      }

      loggedIn <- UserDAO.pushSession(user.itself, ActiveSession(key=session, ip=request.remoteAddress))

      reg <- RegistrationDAO.course.register(user.id, course.id, Set(CourseRole.student), EmptyKind)
    } yield course


    for {
      c <- rCourse.toFuture
    } yield Redirect("/" + path).withSession(request.session + ("sessionKey" -> session))

  }


  def ltiVideoForm(cid:String) = Action.async { request =>

    def getParam(params:Map[String, Seq[String]], name:String) = params.get(name).flatMap(_.headOption)

    def courseContainsLti(c:Course, ck:String) = {
      c.ltis.exists(_.clientKey == ck)
    }

    (for {
    // Get the course if it's available and the details are right
      body <- request.body.asFormUrlEncoded.toRef orIfNone Refused("No LTI data in request")
      name <- getParam(body, "lis_person_name_full").toRef
      email <- getParam(body, "lis_person_contact_email_primary").toRef orIfNone Refused("Couldn't find a user identity (email) in the request")

    } yield {
      new File(s"videolog/").mkdirs()
      val fw = new FileWriter(s"videolog/$cid", true)
      fw.write(s"$cid: $name $email opened the form at ${System.currentTimeMillis()}\n")
      fw.close()

      Ok(views.html.videoForm(course=cid, name=name, code=name.hashCode.toString, email=email))
    }).toFuture

  }

  def ltiVideoSubmit = Action.async(parse.multipartFormData[TemporaryFile](Multipart.handleFilePartAsTemporaryFile, maxLength=1073741824)) { request =>

    def logSave(course:String, name:String, email:String, code:String, randString:String, filename:String) = Try {
      val fw = new FileWriter(s"videolog/$course", true)
      fw.write(s"$course: $name $email with code $code saved file ${code}_${randString}_${filename}\n")
      fw.close()
      true
    }

    def mapSave(course:String, name:String, email:String, code:String, randString:String, filename:String) = Try {
      val mw = new FileWriter(s"videomap.csv", true)
      mw.write(s""""$course","$name","$email","$code","${code}_${randString}_${filename}","${System.currentTimeMillis}"\n""")
      mw.close()
      true
    }

    (for {
      video <- request.body.file("video").toRef
      filename = video.filename
      contentType = video.contentType

      dataParts = request.body.dataParts
      course <- dataParts.get("course").flatMap(_.headOption).toRef orIfNone Refused("No course specified -- please reload")
      code <- dataParts.get("code").flatMap(_.headOption).toRef orIfNone Refused("No verification code in the form -- something went wrong")
      name <- dataParts.get("name").flatMap(_.headOption).toRef orIfNone Refused("No name in the form -- I don't know who you are")
      email <- dataParts.get("email").flatMap(_.headOption).toRef orIfNone Refused("No name in the form -- I don't know who you are")

      randString = scala.util.Random.alphanumeric.take(5).mkString

      logged <- logSave(course, name, email, code, randString, filename).toRef
      mapped <- mapSave(course, name, email, code, randString, filename).toRef
      moved = video.ref.moveTo(new File(s"video/$course/${code}_${randString}_${filename}"))
    } yield {
      Ok("File uploaded")
    }).toFuture.recover { case ex:Exception =>
      Ok("<h1>Oh no! It didn't work! Please email wbilling@une.edu.au</h2><pre>" + ex.getMessage + "</pre>").as("text/html")
    }

  }

}
