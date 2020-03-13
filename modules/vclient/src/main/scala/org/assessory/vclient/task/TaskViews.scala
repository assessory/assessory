package org.assessory.vclient.task

import com.assessory.api.{Target, TargetCourseReg, TargetTaskOutput, Task, TaskBody, TaskOutput, TaskOutputBody}
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{Critique, CritiqueTask}
import com.assessory.api.due.Due
import com.assessory.api.question.{QuestionnaireTask, QuestionnaireTaskOutput}
import com.wbillingsley.handy.{Id, Latch}
import com.wbillingsley.handy.appbase.{Course, Group}
import com.wbillingsley.veautiful.html.{<, DElement, VHtmlComponent, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.services.{GroupService, TaskOutputService, TaskService}
import com.wbillingsley.handy.Ids._
import org.assessory.vclient.common.{Components, Front}
import org.assessory.vclient.course.CourseViews
import org.scalajs.dom.{Element, Node, html}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Date
import TaskService._
import TaskOutputService._
import com.wbillingsley.veautiful.DiffNode

import scala.util.{Failure, Success}

object TaskViews {

  /**
   * The list of tasks on the front page of a course
   */
  def courseTasks(c:Id[Course, String]):VHtmlNode = LatchRender(TaskService.courseTasks(c)) { tasks =>
    <.div(
      for { t <- tasks } yield taskInfo(t)
    )
  }

  /**
   * The information that appears in the Course task list
   */
  def taskInfo(wp:WithPerms[Task]):VHtmlNode = {
    val task = wp.item
    val name = task.details.name.getOrElse("Untitled task")

    <.div(
      <.h3(
        <.a(^.href := Routing.TaskRoute(task.id).path, name)
      ),
      taskAdmin(wp),
      <.p(task.details.description.getOrElse(""):String),
      <.div(
        <.div(^.cls := "text-info", "opens: ", due(task.details.open)),
        <.div(^.cls := "text-danger", "closes: ", due(task.details.closed)),
        <.p()
      )
    )
  }

  /**
   * Converts a due date to a span
   */
  def due(due:Due):VHtmlNode = {
    val groups = Latch.lazily(
      for {
        groups <- GroupService.myGroups.request
      } yield {
        groups.map(_.item.id.id).asIds[Group]
      }
    )

    LatchRender(groups)({ g => optDate(due.due(g)) }, none = <.span())
  }

  /**
   * Text for a date in Unix epoch
   */
  def optDate(o:Option[Long]):DElement[html.Element] = <.span(
    for { d <- o } yield new Date(d).toLocaleString()
  )

  /**
   * Provides administrative links for a task
   */
  def taskAdmin(wp:WithPerms[Task]):VHtmlNode = {
    if (wp.perms("edit")) {
      <.div(
        <.a(^.href := Routing.TaskOutputRoute(wp.item.id).path, "View submissions")
      )
    } else <.div()
  }

  /**
   * The "front page" for a task - letting a user edit or view their entry depending on whether it is open
   * @return
   */
  def taskFront(id:Id[Task, String]):VHtmlNode = LatchRender(TaskService.latch(id)) { wp =>
    val task = wp.item

    <.div(
      Front.siteHeader,

      <.div(^.cls := "container",
        CourseViews.courseInfo(task.course),
        taskInfo(wp),
        if (wp.perms("complete")) {
          editOutputForTask(wp.item)
        } else {
          viewOutputForTask(wp.item)
        }
      )
    )
  }


  def outputLabel(task:Task, taskOutput:TaskOutput):VHtmlNode = taskOutput.body match {
    case c:Critique =>
      <.span(TargetViews.ByLabel(taskOutput.by), " critiques ", TargetViews.ByLabel(c.target))
    case _ =>
      TargetViews.ByLabel(taskOutput.by)
  }


  /**
   * Show a screen where the marker can review all submitted task outputs (whether published or not)
   * @return
   */
  def allOutputs(id:Id[Task, String]):VHtmlNode = {
    LatchRender(TaskService.latch(id), _key="outputs") { wp =>
      val task = wp.item

      <.div(
        Front.siteHeader,

        <.div(^.cls := "container",
          CourseViews.courseInfo(task.course),
          <.h3(task.details.name),
          AllOutputsViewer(task)
        )
      )
    }
  }

  case class AllOutputsViewer(task:Task) extends VHtmlComponent {

    private val outputsLatch = Latch.lazily(TaskOutputService.allOutputs(task.id))

    private var selected:Option[TaskOutput] = None

    def select(o:TaskOutput) = {
      selected = Some(o)
      rerender()
    }

    override protected def render: DiffNode[Element, Node] = {
      <.div(^.cls := "row",
        <.div(^.cls := "col-md-3 scrolling-sidebar",
          LatchRender(outputsLatch) { outputs =>

            <.ul(^.cls := "nav nav-pills flex-column",
              for {
                o <- outputs
              } yield <.li(^.cls := "nav-item",
                <.button(
                  ^.cls := (if (selected.contains(o)) "btn btn-link nav-link active" else "btn btn-link nav-link"),
                  ^.onClick --> select(o),
                  outputLabel(task, o)
                )
              )
            )
          }
        ),
        <.div(^.cls := "col-md-9",
          selected match {
            case Some(output) =>
              viewOutputBody(task.body, output.body)
            case None => <.div()
          }
        )
      )

    }

  }


  def preview(target:Target):VHtmlNode = target match {
    case TargetTaskOutput(id) => Preview(id)
  }


  case class Preview(to:Id[TaskOutput, String]) extends VHtmlComponent {

    val latches = Latch.lazily((for {
      taskOutput <- to.lazily(TaskOutputService.lookup)
      task <- taskOutput.task.lazily(TaskService.lookup)
    } yield (task, task.body, taskOutput, taskOutput.body)).toFuture)

    override protected def render: DiffNode[Element, Node] = <.div(LatchRender(latches) {
      case (t, tb:QuestionnaireTask, to, tob:QuestionnaireTaskOutput) =>
        <.div(QuestionnaireViews.previewAnswers(tb, tob))
    })
  }


  def editOutputForTask(task: Task):VHtmlNode = {
    task.body match {
      case q:QuestionnaireTask => QuestionnaireViews.EditOutputView(task)
      case c:CritiqueTask => CritiqueViews.EditOutputView(task)
      case _ => <.div(s"Edit screen needs writing for ${task.body.getClass.getName}")
    }
  }

  def viewOutputForTask(task: Task):VHtmlNode = {
    task.body match {
      case _ => <.div(s"View screen needs writing for ${task.body.getClass.getName}")
    }
  }

  /**
   * Forwards to the correct renderer for editing a TaskOutput's body. The action buttons (save, publish) are passed
   * on, as some renderers (e.g. for critiques) may need to put the buttons in a different place.
   */
  def editOutputBody(task:TaskBody, taskOutput:TaskOutputBody)(updateBody: TaskOutputBody => Unit, actions: => Seq[VHtmlNode]) = (task, taskOutput) match {
    case (q:QuestionnaireTask, qto:QuestionnaireTaskOutput) =>
      QuestionnaireViews.editAnswers(q, qto)(updateBody, actions)
    case (ct:CritiqueTask, c:Critique) =>
      CritiqueViews.editBody(ct, c)(updateBody, actions)
    case _ =>
      <.div(s"Error: Missing EditBody renderer for ${task.getClass.getName} -> ${taskOutput.getClass.getName}")
  }

  def viewOutputBody(task:TaskBody, taskOutput:TaskOutputBody) = (task, taskOutput) match {
    case (q:QuestionnaireTask, qto:QuestionnaireTaskOutput) =>
      QuestionnaireViews.previewAnswers(q, qto)
    case (c:CritiqueTask, ct:Critique) =>
      CritiqueViews.viewBody(c, ct)
    case _ =>
      <.div(s"Error: Missing ViewBody renderer for ${task.getClass.getName} -> ${taskOutput.getClass.getName}")
  }


  case class EditOutputBody(task:Task, var taskOutput:TaskOutput) extends VHtmlComponent {

    private var status = Latch.immediate(taskOutput)
    status.request
    var modified = false

    def replaceBody(updated:TaskOutputBody):Unit = {
      modified = true
      taskOutput = taskOutput.copy(body=updated)
      rerender()
    }

    private def savable:Boolean = TaskOutputService.isUnsaved(taskOutput) || (status.isCompleted && modified)

    private def available:Boolean = taskOutput.finalised.nonEmpty

    private def notFinalisable:Option[String] = {
      if (available) Some("Already published")
      else if (savable || taskOutput.id == TaskOutputService.invalidId) Some("Needs saving")
      else None
    }

    def save():Unit = {
      if (savable) {
        status = Latch.lazily {
          TaskOutputService.save(taskOutput).map(_.item)
        }

        status.request.onComplete {
          case Success(to) =>
            taskOutput = to
            modified = false
            rerender()
          case Failure(exception) =>
            println("failed")
            rerender()
        }

        rerender()
      }
    }

    def makeAvailable():Unit = {
      if (notFinalisable.isEmpty) {
        status = Latch.lazily {
          TaskOutputService.finalise(taskOutput).map(_.item)
        }

        status.request.onComplete {
          case Success(to) =>
            taskOutput = to
            modified = false
            rerender()
          case Failure(exception) =>
            println("failed")
            rerender()
        }

        rerender()
      }
    }

    private def saveButtons = {
      <.div(^.cls := "form-group",
        <.button(^.cls := "btn btn-default",
          ^.on("click") --> save(),
          ^.attr("disabled") ?= (if (savable) None else Some("disabled")),
          (if (savable) "Save" else if (!status.isCompleted) "(Saving)" else "Already saved")
        ),
        <.button(^.cls := "btn btn-primary",
          ^.on("click") --> makeAvailable(),
          ^.attr("disabled") ?= notFinalisable.map(_ => "disabled"),
          ("" + notFinalisable.getOrElse("Publish"))
        ),
        Components.latchErrorRender(status)
      )
    }

    def render = <.div(
      editOutputBody(task.body, taskOutput.body)(replaceBody, Seq(saveButtons))
    )

  }

}
