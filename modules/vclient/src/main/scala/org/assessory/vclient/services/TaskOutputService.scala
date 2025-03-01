package org.assessory.vclient.services

import com.assessory.api.*
import com.assessory.api.client.WithPerms
import com.assessory.api.question.*
import com.assessory.api.video.*
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles.*
import com.assessory.api.appbase.Course
import com.wbillingsley.handy.{EagerLookUpOne, EagerLookUpOpt, Id, Latch, LookUp, Ref, RefMany, lazily, refOps}
import org.assessory.vclient.Routing
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import Ref.*
import com.assessory.api.call.{ReturnTaskOutput, TaskOutputCall, CritiqueCall, ReturnTarget, StandardReturn}

object TaskOutputService {

  given EagerLookUpOne[Id[TaskOutput, String], TaskOutput] = (id:Id[TaskOutput, String]) =>
    latch(id.id).request.map(_.item).toRef

  val invalidId = TaskOutputId("invalid")

  val cache = mutable.Map.empty[String, Future[WithPerms[TaskOutput]]]

  UserService.self.addListener { _ => cache.clear() }

  def isUnsaved(to:TaskOutput):Boolean = to.id == invalidId

  def myOutputs(taskId:TaskId):Future[Seq[TaskOutput]] = {
    for case StandardReturn.ReturnMany(items) <- callClient.makeCall(TaskOutputCall.MyOutputs(taskId)) yield
      for case ReturnTaskOutput(to) <- items yield to
  }

  def allOutputs(taskId:TaskId):Future[Seq[TaskOutput]] = {
    for case StandardReturn.ReturnMany(items) <- callClient.makeCall(TaskOutputCall.AllOutputs(taskId)) yield
      for case ReturnTaskOutput(to) <- items yield to
  }

  def updateBody(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    val fto = for
      case StandardReturn.ReturnWithPermissions(ReturnTaskOutput(t), perms) <- callClient.makeCall(TaskOutputCall.UpdateBody(to))
    yield
      WithPerms(perms, t)
    cache.put(to.id.id, fto)
    fto
  }

  def finalise(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    val fto = for
      case StandardReturn.ReturnWithPermissions(ReturnTaskOutput(t), perms) <- callClient.makeCall(TaskOutputCall.Finalise(to))
    yield
      WithPerms(perms, t)
    cache.put(to.id.id, fto)
    fto
  }

  def createNew(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    val fto = for
      case StandardReturn.ReturnWithPermissions(ReturnTaskOutput(t), perms) <- callClient.makeCall(TaskOutputCall.CreateTaskOutput(to))
    yield
      WithPerms(perms, t)
    cache.put(to.id.id, fto)
    fto
  }

  def save(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    if (isUnsaved(to)) createNew(to) else updateBody(to)
  }

  def loadId(id:TaskOutputId):Future[WithPerms[TaskOutput]] = {
    for
      case StandardReturn.ReturnWithPermissions(ReturnTaskOutput(t), perms) <- callClient.makeCall(TaskOutputCall.GetTaskOutput(id))
    yield
      WithPerms(perms, t)
  }

  def latch(s:String):Latch[WithPerms[TaskOutput]] = latch(TaskOutputId(s))

  def latch(id:Id[TaskOutput,String]):Latch[WithPerms[TaskOutput]] = Latch.lazily(cache.getOrElseUpdate(id.id, loadId(TaskOutputId(id.id))))

  def future(id:Id[TaskOutput,String]):Future[WithPerms[TaskOutput]] = cache.getOrElseUpdate(id.id, loadId(TaskOutputId(id.id)))


  /**
   * Creates a blank answer for a given task
   * @return
   */
  def blankOutputFor(t:Task, by:By):TaskOutput = {
    TaskOutput(
      id = invalidId,
      task = t.id,
      by = by,
      body = emptyBodyFor(t.body)
    )
  }

  def emptyBodyFor(tb:TaskBody):TaskOutputBody = tb match {
    case v:VideoTask => VideoTaskOutput(None)
    case m:MessageTask => MessageTaskOutput(m.text)
    case f:SmallFileTask => SmallFileTaskOutput(None)
    case c:CompositeTask => CompositeTaskOutput(c.tasks.map(emptyBodyFor))
    case q:QuestionnaireTask => QuestionnaireTaskOutput(q.questionnaire.map(_.blankAnswer))
    case _ => throw new MatchError("Could not create empty task body for " + tb)
  }


  def myAllocations(taskId:TaskId):Future[Seq[Target]] = {
    for case StandardReturn.ReturnMany(items) <- callClient.makeCall(CritiqueCall.MyAllocations(taskId)) yield
      for case ReturnTarget(t) <- items yield t
  }

  def fillAllocations(taskId:TaskId):Future[Seq[TaskOutput]] = {
    for case StandardReturn.ReturnMany(items) <- callClient.makeCall(CritiqueCall.FillMyTaskOutputs(taskId)) yield
      for case ReturnTaskOutput(t) <- items yield t
  }

  def findOrCreateCrit(taskId:TaskId, target:Target):Future[TaskOutput] = {
    val fwp = for
      case StandardReturn.ReturnWithPermissions(ReturnTaskOutput(to), perms) <- callClient.makeCall(CritiqueCall.FindOrCreateCritique(taskId, target))
    yield WithPerms(perms, to)
    for { wp <- fwp } yield {
      cache.put(wp.item.id.id, fwp)
      wp.item
    }
  }

}
