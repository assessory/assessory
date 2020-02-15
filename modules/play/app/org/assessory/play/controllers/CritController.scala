package org.assessory.play.controllers

import com.assessory.api._
import com.assessory.api.critique._
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import com.assessory.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy._
import play.api.mvc._
import util.RefConversions._
import util.UserAction
import Pickles._
import com.wbillingsley.handy.appbase.UserError
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object CritController {
  implicit def caToResult(rc:Ref[CritAllocation])(implicit ec: ExecutionContext):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def targetToResult(rc:Ref[Target])(implicit ec: ExecutionContext):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def manyCAToResult(rc:RefMany[CritAllocation])(implicit ec: ExecutionContext):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyTargetToResult(rc:RefMany[Target])(implicit ec: ExecutionContext):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }
}

class CritController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  import CritController._

  def allocateTask(taskId:String) = userAction.async { implicit request =>
    CritModel.allocateTask(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )
  }


  def myAllocations(taskId:String) = userAction.async { implicit request =>
    manyTargetToResult(
      CritModel.myAllocations(request.approval, LazyId(taskId).of[Task])
    )
  }

  def allocations(taskId:String) = userAction.async { implicit request =>
    manyCAToResult(
      CritModel.allocations(LazyId(taskId).of[Task])
    )
  }

  def findOrCreateCrit(taskId:String) = userAction.async { implicit request =>
    import TaskOutputController._

    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to find/create critique had no body to parse")
      client <- Pickles.read[Target](text).toRef
      wp <- CritModel.findOrCreateCrit(
        a = request.approval,
        rTask = LazyId(taskId).of[Task],
        target = client
      )
    } yield wp

    wptoToResult(wp)
  }

  /** Fetches allocations as a CSV. */
  def allocationsAsCSV(taskId:String) = userAction.async { implicit request =>
    val lines = CritModel.allocationsAsCSV(
      a = request.approval,
      rTask = LazyId(taskId).of[Task]
    )

    lines.map(Ok(_).as("application/csv")).toFuture
  }

  def taskOutputsFor(taskId:String) = userAction.async { implicit request =>
    TaskOutputController.manyTaskOutputToResult(
      for {
        t <- taskId.asId[Task].lazily
        a <- request.approval ask Permissions.ViewTask(t.itself)
        to <- CritModel.makeTos(request.approval, t)
      } yield to
    )
  }
}
