package org.assessory.vclient.task

import com.assessory.api.critique.{Critique, CritiqueTask}
import com.assessory.api.{TargetGroup, Task, TaskBody, TaskOutput, TaskOutputBody}
import com.wbillingsley.handy.Latch
import com.wbillingsley.veautiful.DiffNode
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode, ^}
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.group.GroupViews
import org.assessory.vclient.services.TaskOutputService
import org.scalajs.dom.{Element, Node}

import scala.concurrent.ExecutionContext.Implicits.global

object CritiqueViews {

  /**
   * Edit view for critique tasks
   */
  case class EditOutputView(task:Task) extends VHtmlComponent {

    private val allocations = TaskOutputService.taskOutputsFor(task.id)
    private val allocationsLatch = Latch.lazily(allocations)

    private var selected:Option[TaskOutput] = None

    private def select(o:Option[TaskOutput]):Unit = {
      selected = o
      rerender()
    }

    def render = {


      <.div(
        LatchRender(allocationsLatch) { all =>
          <.ul(^.cls := "nav nav-pills", ^.role := "group",
            for ((to, idx) <- all.zipWithIndex) yield {
              <.li(^.cls := "nav-item" , ^.role := "presentation",
                <.button(^.cls := (if (selected.contains(to)) "btn btn-link nav-link active" else "btn btn-link nav-link"),  ^.onClick --> select(Some(to)), (idx + 1).toString)
              )
            }
          )
        },
        (for { to <- selected } yield TaskViews.EditOutputBody(task, to))
      )
    }
  }


  def editBody(critiqueTask: CritiqueTask, critique: Critique)(updateBody: TaskOutputBody => Unit, actions: => Seq[VHtmlNode]):VHtmlNode = {
    <.div(^.cls := "row",
      <.div(^.cls := "col",
        <.div(^.cls := "card mt-1 mb-1",
          <.div(^.cls := "card-header", "What you are reviewing"),
          <.div(^.cls := "card-body",
            TaskViews.preview(critique.target)
          )
        )
      ),
      <.div(^.cls := "col",
        <.div(^.cls := "card",
          <.div(^.cls := "card-header", "Your critique"),
          <.div(^.cls := "card-body",
            TaskViews.editOutputBody(critiqueTask.task, critique.task)({ t => updateBody(critique.copy(task = t))}, actions)
          )
        )
      )
    )
  }


  def viewBody(critiqueTask: CritiqueTask, critique: Critique):VHtmlNode = {
    <.div(^.cls := "card",
      <.div(^.cls := "card-header", "Your critique"),
      <.div(^.cls := "card-body",
        TaskViews.viewOutputBody(critiqueTask.task, critique.task)
      )
    )
  }


}
