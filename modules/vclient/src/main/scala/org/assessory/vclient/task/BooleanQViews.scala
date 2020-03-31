package org.assessory.vclient.task

import com.assessory.api.question.{BooleanAnswer, Question, ShortTextAnswer}
import com.wbillingsley.veautiful.html.{<, VHtmlNode, ^}
import org.assessory.vclient.common.Components._
import org.scalajs.dom

/**
 * Views for rendering Boolean (yes/no) Questions
 */
object BooleanQViews {

  def editBooleanAnswer(q:Question, a:BooleanAnswer)(f: BooleanAnswer => Unit):VHtmlNode = {
    def updateAnswer(ans:Boolean):Unit = {
      f(a.copy(answer=Some(ans)))
    }

    <.div(^.cls := "form-group",
      <.div(^.cls := "form-check form-check-inline",
        <.input(^.attr("type") := "radio", ^.cls := "form-check-input",
          if (a.answer.contains(true)) ^.attr("checked") := "checked" else Seq.empty,
          ^.on("change") ==> { evt => evt.checked.foreach(updateAnswer) }
        ),
        <("label")(^.cls := "form-check-label", "Yes"),
      ),
      <.div(^.cls := "form-check form-check-inline",
        <.input(^.attr("type") := "radio", ^.cls := "form-check-input",
          if (a.answer.contains(false)) ^.attr("checked") := "checked" else Seq.empty,
          ^.on("change") ==> { evt => evt.checked.foreach(x => updateAnswer(!x)) }
        ),
        <("label")(^.cls := "form-check-label", "No")
      )
    )
  }

  def viewBooleanAnswer(q:Question, a:BooleanAnswer):VHtmlNode = {
    <.div(^.cls := "form-group",
      <.div(^.cls := "form-check form-check-inline",
        <.input(^.attr("type") := "radio", ^.cls := "form-check-input", ^.attr("read-only") := "true",
          if (a.answer.contains(true)) ^.attr("checked") := "checked" else Seq.empty
        ),
        <("label")(^.cls := "form-check-label", "Yes"),
      ),
      <.div(^.cls := "form-check form-check-inline",
        <.input(^.attr("type") := "radio", ^.cls := "form-check-input", ^.attr("read-only") := "true",
          if (a.answer.contains(false)) ^.attr("checked") := "checked" else Seq.empty
        ),
        <("label")(^.cls := "form-check-label", "No")
      )
    )
  }

}
