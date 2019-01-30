package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.client.{CreateCoursePreenrolData, WithPerms}
import com.assessory.api.wiring.Lookups._
import com.assessory.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{Course, UserError}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import util.{RefConversions, UserAction}
import RefConversions._
import Id._
import com.assessory.clientpickle.Pickles
import Pickles._
import javax.inject.Inject

import scala.concurrent.Future
import scala.language.implicitConversions

object CourseController {
  implicit def courseToResult(rc:Ref[Course]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def wpcToResult(rc:Ref[WithPerms[Course]]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def manyCourseToResult(rc:RefMany[Course]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWpcToResult(rc:RefMany[WithPerms[Course]]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }
}

class CourseController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)
  extends AbstractController(cc) {

  import CourseController._

  /**
   * Retrieves a course
   */
  def get(id:String) = userAction.async { implicit request =>
    CourseModel.byId(request.approval, id.asId)
  }

  /**
   * Creates a course
   */
  def create = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to create course had no body to parse")
      clientCourse <- Pickles.read[Course](text).toRef
      wp <- CourseModel.create(request.approval, clientCourse)
    } yield wp

    wp
  }

  /**
   * Retrieves a course
   */
  def findMany = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      ids <- Pickles.read[Ids[Course,String]](text).toRef
      wp <- CourseModel.findMany(request.approval, ids)
    } yield wp

    wp
  }


  /**
   * Fetches the courses this user is registered with.
   * Note that this also performs the pre-enrolments
   */
  def myCourses = userAction.async { implicit request =>
    CourseModel.myCourses(request.approval)
  }


  /**
   * Generates a CSV of the autologin links
   */
  def autolinks(courseId:String) = userAction.async { implicit request =>
    val lines = for {
      u <- CourseModel.usersInCourse(request.approval, courseId.asId)
      c <- courseId.asId[Course].lazily

      // TODO: Which Identity do we want?
      optStudentIdent <- u.identities.headOption.toRef
      studentIdent <- optStudentIdent.value.toRef
      url = routes.UserController.autologin(u.id.id, u.secret).absoluteURL()
    } yield s"${studentIdent},$url\n"

    for {
      e <- lines.toFutureSource
    } yield Ok.chunked(e).as("application/csv")

  }

}
