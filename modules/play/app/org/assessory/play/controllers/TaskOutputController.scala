package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import com.assessory.model.TaskOutputModel
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import play.api.mvc._
import util.RefConversions._
import util.UserAction

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import Pickles._
import com.wbillingsley.handy.appbase.UserError
import javax.inject.Inject

object TaskOutputController {
  implicit def taskOutputToResult(rc:Ref[TaskOutput])(implicit ec:ExecutionContext):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def wptoToResult(rc:Ref[WithPerms[TaskOutput]])(implicit ec:ExecutionContext):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def manyTaskOutputToResult(rc:RefMany[TaskOutput])(implicit ec:ExecutionContext):Future[Result] = {
    val strings = rc.map(c => {
      try {
        val p = Pickles.write(c)
        p
      } catch {
        case x:Exception => x.printStackTrace
          throw x
      }
    })

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWptoToResult(rc:RefMany[WithPerms[TaskOutput]])(implicit ec:ExecutionContext):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

}

class TaskOutputController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)(implicit ec:ExecutionContext)
  extends AbstractController(cc) {

  import TaskOutputController._

  def get(id:String) = userAction.async { implicit request =>
    TaskOutputModel.get(
      request.approval,
      id.asId
    )
  }

  def myOutputs(taskId:String) = userAction.async { implicit request =>
    TaskOutputModel.myOutputs(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )
  }

  def allOutputs(taskId:String) = userAction.async { implicit request =>
    TaskOutputModel.allOutputs(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )
  }

  def create(taskId:String) = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to create TaskOutput had no body to parse")
      client <- Pickles.read[TaskOutput](text).toRef
      wp <- TaskOutputModel.create(
        a = request.approval,
        task = LazyId(taskId).of[Task],
        clientTaskOutput = client,
        finalise = false // TODO: allow finalising of tasks
      )
    } yield wp

    wp
  }

  def updateBody(id:String) = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to update TaskOutput had no body to parse")
      client <- Pickles.read[TaskOutput](text).toRef
      wp <- TaskOutputModel.updateBody(
        a = request.approval,
        clientTaskOutput = client,
        finalise = false // TODO: allow finalising of tasks
      ).require
    } yield wp

    wp
  }

  def finalise(id:String) = userAction.async { implicit request =>
    TaskOutputModel.finalise(request.approval, id.asId[TaskOutput].lazily)
  }

  /** Fetches allocations as a CSV. */
  def outputsAsCSV(taskId:String) = userAction.async { implicit request =>
    val lines = TaskOutputModel.asCsv(
      request.approval,
      taskId.asId
    )

    lines.map(Ok(_).as("application/csv")).toFuture
  }
}
