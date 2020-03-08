package org.assessory.vclient.task

import com.assessory.api.critique.{Critique, CritiqueTask}
import com.assessory.api.{TargetGroup, Task, TaskOutput}
import com.wbillingsley.handy.Latch
import com.wbillingsley.veautiful.DiffNode
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, ^}
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
                <.button(^.cls := (if (selected.contains(to)) "nav-link active" else "nav-link"),  ^.onClick --> select(Some(to)), (idx + 1).toString)
              )
            }
          )
        },
        (task.body -> selected.map(_.body)) match {
          case (ct:CritiqueTask, Some(crit:Critique)) =>
            <.div(^.cls := "row",
              <.div(^.cls := "col",
                <.div(^.cls := "card",
                  <.div(^.cls := "card-header", "What you are reviewing"),
                  <.div(^.cls := "card-body",
                    TaskViews.preview(crit.target)
                  )
                )
              ),
              <.div(^.cls := "col",
                <.h3("Your critique")
              )
            )
          case (_, None) => <.div()
          case (tb, Some(x)) => <.div(s"Error: Either not a critique task or not a critique ${tb.getClass.getName} ${x.getClass.getName}")
        }
      )
    }
  }


}
