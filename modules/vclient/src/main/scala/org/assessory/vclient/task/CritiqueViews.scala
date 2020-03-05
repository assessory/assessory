package org.assessory.vclient.task

import com.assessory.api.critique.Critique
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

    def render = <.div(
      LatchRender(allocationsLatch) { all =>
        <.ul(^.cls := "nav nav-pills", ^.role := "group",
          for ((to, idx) <- all.zipWithIndex) yield {
            <.li(^.cls := (if (selected.contains(to)) "active" else ""), ^.role := "presentation",
              <.a(^.onClick --> select(Some(to)), idx.toString)
            )
          }
        )
      },

    )
  }


}
