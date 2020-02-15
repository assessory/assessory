package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import com.assessory.model._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{Course, UserError}
import play.api.mvc._
import util.{RefConversions, UserAction}
import RefConversions._
import Pickles._
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object TaskController {

  implicit def taskToResult(rc:Ref[Task])(implicit ec:ExecutionContext):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def wptToResult(rc:Ref[WithPerms[Task]])(implicit ec:ExecutionContext):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def manyTaskToResult(rc:RefMany[Task])(implicit ec:ExecutionContext):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWptToResult(rc:RefMany[WithPerms[Task]])(implicit ec:ExecutionContext):Future[Result] = {

    val strings = rc.map({c => Pickles.write(c) })

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

}

class TaskController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)(implicit ec:ExecutionContext)
  extends AbstractController(cc) {

  import TaskController._

  def get(id:String) = userAction.async { implicit request =>
    TaskModel.byId(request.approval, id.asId)
  }
  
  
  def create(courseId:String) = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to create task had no body to parse")
      client <- Pickles.read[Task](text).toRef
      wp <- TaskModel.create(request.approval, client)
    } yield wp

    wptToResult(wp)
  }
  
  def updateBody(taskId:String) = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to update task had no body to parse")
      client <- Pickles.read[Task](text).toRef
      wp <- TaskModel.updateBody(request.approval, client)
    } yield wp

    wptToResult(wp.require)
  }
  
  def courseTasks(courseId:String) = userAction.async { implicit request =>
    manyWptToResult(
      TaskModel.courseTasks(
        a = request.approval,
        rCourse = LazyId(courseId).of[Course]
      )
    )
  }

}