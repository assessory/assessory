package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import com.assessory.model._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.Course
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller, Result, Results}
import util.{UserAction, RefConversions}
import RefConversions._
import Pickles._

import scala.concurrent.Future
import scala.language.implicitConversions

class TaskController extends Controller {

  implicit def taskToResult(rc:Ref[Task]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def wptToResult(rc:Ref[WithPerms[Task]]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def manyTaskToResult(rc:RefMany[Task]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWptToResult(rc:RefMany[WithPerms[Task]]):Future[Result] = {
    val strings = rc.map({c => upickle.default.write(c) })

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  def get(id:String) = UserAction.async { implicit request =>
    TaskModel.byId(request.approval, id.asId)
  }
  
  
  def create(courseId:String) = UserAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      client = upickle.default.read[Task](text)
      wp <- TaskModel.create(request.approval, client)
    } yield wp

    wptToResult(wp)
  }
  
  def updateBody(taskId:String) = UserAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      client = upickle.default.read[Task](text)
      wp <- TaskModel.updateBody(request.approval, client)
    } yield wp

    wptToResult(wp)
  }
  
  def courseTasks(courseId:String) = UserAction.async { implicit request =>
    manyWptToResult(
      TaskModel.courseTasks(
        a = request.approval,
        rCourse = LazyId(courseId).of[Course]
      )
    )
  }

}