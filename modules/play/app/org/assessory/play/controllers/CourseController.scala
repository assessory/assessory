package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.client.{CreateCoursePreenrolData, WithPerms}
import com.assessory.api.wiring.Lookups._
import com.assessory.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.Course
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller, Result, Results}
import util.{UserAction, RefConversions}
import RefConversions._
import Id._

import scala.concurrent.Future
import scala.language.implicitConversions

class CourseController extends Controller {


  implicit def courseToResult(rc:Ref[Course]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def wpcToResult(rc:Ref[WithPerms[Course]]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def manyCourseToResult(rc:RefMany[Course]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWpcToResult(rc:RefMany[WithPerms[Course]]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  /**
   * Retrieves a course
   */
  def get(id:String) = UserAction.async { implicit request =>
    CourseModel.byId(request.approval, id.asId)
  }

  /**
   * Creates a course
   */
  def create = UserAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      clientCourse = upickle.default.read[Course](text)
      wp <- CourseModel.create(request.approval, clientCourse)
    } yield wp

    wp
  }

  /**
   * Retrieves a course
   */
  def findMany = UserAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      ids = upickle.default.read[Ids[Course,String]](text)
      wp <- CourseModel.findMany(request.approval, ids)
    } yield wp

    wp
  }


  /**
   * Fetches the courses this user is registered with.
   * Note that this also performs the pre-enrolments
   */
  def myCourses = UserAction.async { implicit request =>
    CourseModel.myCourses(request.approval)
  }


  /**
   * Generates a CSV of the autologin links
   */
  def autolinks(courseId:String) = UserAction.async { implicit request =>
    val lines = for {
      u <- CourseModel.usersInCourse(request.approval, courseId.asId)
      optStudentIdent <- u.getIdentity(I_STUDENT_NUMBER).toRef
      studentIdent <- optStudentIdent.value.toRef
      url = routes.UserController.autologin(u.id.id, u.secret).absoluteURL()
    } yield s"${studentIdent},$url\n"

    for {
      e <- lines.toFutureSource
    } yield Ok.chunked(e).as("application/csv")

  }

}
