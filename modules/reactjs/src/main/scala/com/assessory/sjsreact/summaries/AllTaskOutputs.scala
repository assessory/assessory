package com.assessory.sjsreact.summaries

import com.assessory.api._
import com.assessory.sjsreact.{TargetViews, GroupViews, WebApp }
import com.assessory.sjsreact.services.TaskOutputService
import com.wbillingsley.handy.{Latch, Id}
import japgolly.scalajs.react.{Callback, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  *
  */
object AllTaskOutputs {

  case class Selection[T,C](private var _selected: Option[Int], seq:Seq[T], context:C) {

    def selected_=(o:Option[Int]) = {
      this._selected = o
      WebApp.rerender()
    }

    def selected = _selected
  }

  case class Saveable[T,R](item: T, l:Latch[R])


  val allocationsSwitch = ReactComponentB[Selection[Target,TaskOutput]]("outputSelection")
    .render_P { sel =>
      <.ul(^.className := "nav nav-pills", ^.role := "group",
        for ((targ, idx) <- sel.seq.zipWithIndex) yield {
          <.li(^.className := (if (sel.selected.contains(idx)) "active" else ""), ^.role := "presentation",
            <.a(^.onClick --> Callback { sel.selected = Some(idx) },
              TargetViews.name(targ)
            )
          )
        }
      )
    }
    .build


}
