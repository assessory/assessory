package org.assessory.vclient.services

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.question._
import com.assessory.api.video._
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.appbase.Course
import com.wbillingsley.handy.{Id, Latch}
import org.assessory.vclient.Routing
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object TaskOutputService {

  val cache = mutable.Map.empty[String, Future[WithPerms[TaskOutput]]]

  UserService.self.addListener { _ => cache.clear() }

  def courseTasks(courseId:Id[Course,String]):Latch[Seq[WithPerms[Task]]] = Latch.lazily(
    Ajax.get(s"/api/course/${courseId.id}/tasks", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[WithPerms[Task]]])
  )

  def myOutputs(taskId:Id[Task,String]):Future[Seq[TaskOutput]] = {
    Ajax.get(s"/api/task/${taskId.id}/myOutputs", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[TaskOutput]]).captureUserError
  }

  def myAllocations(taskId:Id[Task,String]):Future[Seq[Target]] = {
    Ajax.get(s"/api/critique/${taskId.id}/myAllocations", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[Target]])
  }

  def taskOutputsFor(taskId:Id[Task,String]):Future[Seq[TaskOutput]] = {
    Ajax.post(s"/api/critique/${taskId.id}/taskOutputsFor", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[TaskOutput]])
  }

  def allOutputs(taskId:Id[Task,String]):Future[Seq[TaskOutput]] = {
    Ajax.post(s"/api/task/${taskId.id}/allOutputs", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[Seq[TaskOutput]])
  }


  def findOrCreateCrit(taskId:Id[Task,String], target:Target):Future[TaskOutput] = {
    val fwp = Ajax.post(s"/api/critique/${taskId.id}/findOrCreateCrit", Pickles.write(target), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[WithPerms[TaskOutput]])
    fwp.onComplete(_ => Routing.Router.rerender())
    for { wp <- fwp } yield {
      cache.put(wp.item.id.id, fwp)
      wp.item
    }
  }

  def updateBody(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    val fto = Ajax.post(s"/api/taskoutput/${to.id.id}", Pickles.write(to), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[WithPerms[TaskOutput]])
    cache.put(to.id.id, fto)
    fto
  }

  def finalise(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    val fto = Ajax.post(s"/api/taskoutput/${to.id.id}/finalise", Pickles.write(to), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[WithPerms[TaskOutput]])
    cache.put(to.id.id, fto)
    fto
  }

  def createNew(to:TaskOutput):Future[WithPerms[TaskOutput]] = {
    val fto = Ajax.post(s"/api/task/${to.task.id}/newOutput", Pickles.write(to), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[WithPerms[TaskOutput]])
    cache.put(to.id.id, fto)
    fto
  }

  def loadId[KK <: String](id:Id[TaskOutput,KK]):Future[WithPerms[TaskOutput]] = {
    val fwp = Ajax.get(s"/api/taskoutput/${id.id}", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[WithPerms[TaskOutput]])
    fwp.onComplete(_ => Routing.Router.rerender())
    fwp
  }

  def latch(s:String):Latch[WithPerms[TaskOutput]] = latch(s.asId)

  def latch(id:Id[TaskOutput,String]):Latch[WithPerms[TaskOutput]] = Latch.lazily(cache.getOrElseUpdate(id.id, loadId(id)))

  def future(id:Id[TaskOutput,String]):Future[WithPerms[TaskOutput]] = cache.getOrElseUpdate(id.id, loadId(id))


  /**
   * Creates a blank answer for a given task
   * @return
   */
  def blankOutputFor(t:Task, by:Target):TaskOutput = {
    TaskOutput(
      id = "invalidId".asId,
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

}
