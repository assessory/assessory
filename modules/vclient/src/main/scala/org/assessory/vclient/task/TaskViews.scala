package org.assessory.vclient.task

import com.assessory.api.Task
import com.assessory.api.client.WithPerms
import com.assessory.api.due.Due
import com.wbillingsley.handy.{Id, Latch}
import com.wbillingsley.handy.appbase.{Course, Group}
import com.wbillingsley.veautiful.html.{<, DElement, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.services.{GroupService, TaskService}
import com.wbillingsley.handy.Ids._
import org.assessory.vclient.common.Front
import org.assessory.vclient.course.CourseViews
import org.scalajs.dom.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Date

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

    LatchRender(groups) { g => optDate(due.due(g)) }
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

  def editOutputForTask(task: Task):VHtmlNode = {
    task.body match {
      case _ => <.div(s"Edit screen needs writing for ${task.body.getClass.getName}")
    }
  }

  def viewOutputForTask(task: Task):VHtmlNode = {
    task.body match {
      case _ => <.div(s"View screen needs writing for ${task.body.getClass.getName}")
    }
  }

}
