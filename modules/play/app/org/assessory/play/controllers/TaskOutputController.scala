package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import com.assessory.model.TaskOutputModel
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import util.RefConversions._
import util.UserAction

import scala.concurrent.Future
import scala.language.implicitConversions
import Pickles._

object TaskOutputController {
  implicit def taskOutputToResult(rc:Ref[TaskOutput]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def wptoToResult(rc:Ref[WithPerms[TaskOutput]]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def manyTaskOutputToResult(rc:RefMany[TaskOutput]):Future[Result] = {
    val strings = rc.map(c => {
      try {
        val p = upickle.default.write(c)
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

  implicit def manyWptoToResult(rc:RefMany[WithPerms[TaskOutput]]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

}

class TaskOutputController extends Controller {

  import TaskOutputController._

  def get(id:String) = UserAction.async { implicit request =>
    TaskOutputModel.get(
      request.approval,
      id.asId
    )
  }

  def myOutputs(taskId:String) = UserAction.async { implicit request =>
    TaskOutputModel.myOutputs(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )
  }

  def create(taskId:String) = UserAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      client = upickle.default.read[TaskOutput](text)
      wp <- TaskOutputModel.create(
        a = request.approval,
        task = LazyId(taskId).of[Task],
        clientTaskOutput = client,
        finalise = false // TODO: allow finalising of tasks
      )
    } yield wp

    wp
  }

  def updateBody(id:String) = UserAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      client = upickle.default.read[TaskOutput](text)
      wp <- TaskOutputModel.updateBody(
        a = request.approval,
        clientTaskOutput = client,
        finalise = false // TODO: allow finalising of tasks
      )
    } yield wp

    wp
  }

  /** Fetches allocations as a CSV. */
  def outputsAsCSV(taskId:String) = UserAction.async { implicit request =>
    val lines = TaskOutputModel.asCsv(
      request.approval,
      taskId.asId
    )

    lines.map(Ok(_).as("application/csv")).toFuture
  }
}
