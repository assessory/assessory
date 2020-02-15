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

  def optDate(o:Option[Long]):DElement[html.Element] = <.span(
    for { d <- o } yield new Date(d).toLocaleString()
  )

  def taskAdmin(wp:WithPerms[Task]):VHtmlNode = {
    if (wp.perms("edit")) {
      <.div(
        <.a(^.href := Routing.TaskOutputRoute(wp.item.id).path, "View submissions")
      )
    } else <.div()
  }

}
