package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.critique._
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import com.assessory.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Controller, Result, Results}
import util.RefConversions._
import util.UserAction
import Pickles._

import scala.concurrent.Future
import scala.language.implicitConversions

object CritController {
  implicit def caToResult(rc:Ref[CritAllocation]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def targetToResult(rc:Ref[Target]):Future[Result] = {
    rc.map(c => Results.Ok(upickle.default.write(c)).as("application/json")).toFuture
  }

  implicit def manyCAToResult(rc:RefMany[CritAllocation]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyTargetToResult(rc:RefMany[Target]):Future[Result] = {
    val strings = rc.map(c => upickle.default.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }
}

class CritController extends Controller {

  import CritController._

  def allocateTask(taskId:String) = UserAction.async { implicit request =>
    CritModel.allocateTask(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )
  }


  def myAllocations(taskId:String) = UserAction.async { implicit request =>
    manyTargetToResult(
      CritModel.myAllocations(request.approval, LazyId(taskId).of[Task])
    )
  }

  def allocations(taskId:String) = UserAction.async { implicit request =>
    manyCAToResult(
      CritModel.allocations(LazyId(taskId).of[Task])
    )
  }

  def findOrCreateCrit(taskId:String) = UserAction.async { implicit request =>
    import TaskOutputController._

    def wp = for {
      text <- request.body.asText.toRef
      client = upickle.default.read[Target](text)
      wp <- CritModel.findOrCreateCrit(
        a = request.approval,
        rTask = LazyId(taskId).of[Task],
        target = client
      )
    } yield wp

    wptoToResult(wp)
  }

  /** Fetches allocations as a CSV. */
  def allocationsAsCSV(taskId:String) = UserAction.async { implicit request =>
    val lines = CritModel.allocationsAsCSV(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )

    lines.map(Ok(_).as("application/csv")).toFuture
  }
}
