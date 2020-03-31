package org.assessory.vclient.task

import com.assessory.api.question.{Question, ShortTextAnswer}
import com.wbillingsley.veautiful.html.{<, VHtmlNode, ^}
import org.assessory.vclient.common.Components._

/**
 * Views for rendering Short Text Questions
 */
object ShortTextQViews {

  def editShortTextAnswer(q:Question, a:ShortTextAnswer)(f: ShortTextAnswer => Unit):VHtmlNode = {
    def updateAnswer(ans:String):Unit = {
      f(a.copy(answer=Some(ans)))
    }

    <.div(^.cls := "form-group",
      <.textarea(^.cls := "form-control", ^.prop("value") ?= a.answer, ^.attr("rows") := 10,
        ^.on("input") ==> { _.inputValue.foreach(updateAnswer) }
      )
    )
  }

  def viewShortTextAnswer(q:Question, a:ShortTextAnswer):VHtmlNode = {
    <.div(^.cls := "form-group",
      <.textarea(^.cls := "form-control", ^.prop("value") := a.answer.getOrElse(""), ^.attr("rows") := 10, ^.attr("readonly") := "readonly")
    )
  }

}
