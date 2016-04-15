package com.assessory.sjsreact

import com.assessory.api.{TargetTaskOutput, Task, Target}
import com.wbillingsley.handy.appbase._
import japgolly.scalajs.react.{Callback, ReactEventI, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

object QuestionViews {

  val viewShortTextA = ReactComponentB[(ShortTextQuestion,ShortTextAnswer)]("viewBooleanA")
    .render_P { tuple =>
    val (q,a) = tuple

    <.div(
      <.textarea(^.className := "form-control", ^.value := (a.answer.getOrElse(""):String), ^.disabled:=true)
    )
  }.build

  val viewBooleanA = ReactComponentB[(BooleanQuestion,BooleanAnswer)]("questionnaireEdit")
    .render_P({ tuple =>
    val (q,a) = tuple

    <.div(
      <.label(^.className := "radio-inline", <.input(^.`type` := "radio",
        ^.checked := a.answer == Some(true), ^.disabled := true, "Yes")
      ),
      <.label(^.className := "radio-inline", <.input(^.`type` := "radio",
        ^.checked := a.answer == Some(false), ^.disabled := true, "No")
      )
    )
  }).build

  val editShortTextA = ReactComponentB[(ShortTextQuestion,ShortTextAnswer)]("questionnaireEdit")
    .render_P { tuple =>
      val (q,a) = tuple

      <.div(
        <.textarea(^.className := "form-control", ^.value := (a.answer.getOrElse(""):String),
          ^.onChange ==> { (evt:ReactEventI) => Callback { a.answer = Some(evt.target.value); WebApp.rerender() } }
        )
      )
    }
    .build

  val editBooleanA = ReactComponentB[(BooleanQuestion,BooleanAnswer)]("questionnaireEdit")
    .render_P ({ tuple =>
    val (q,a) = tuple

    <.div(
      <.label(^.className := "radio-inline", <.input(^.`type` := "radio",
        ^.checked := a.answer.contains(true),
        ^.onChange ==> { (evt:ReactEventI) => Callback { a.answer = Some(evt.target.checked); WebApp.rerender() } } , "Yes")
      ),
      <.label(^.className := "radio-inline", <.input(^.`type` := "radio",
        ^.checked := a.answer == Some(false),
        ^.onChange ==> { (evt:ReactEventI) => Callback { a.answer = Some(!evt.target.checked); WebApp.rerender() } }, "No")
      )
    )
  }).build



  val editQuestionnaireAnswers = ReactComponentB[Seq[(Question,Answer[_])]]("editQuestionnaireAnswers")
    .render_P ({ seq =>
      <.div(
        for (pair <- seq) yield pair match {
          case (q:ShortTextQuestion, a:ShortTextAnswer) => <.div(^.className:="form-group", <.label(q.prompt), editShortTextA((q,a)))
          case (q:BooleanQuestion, a:BooleanAnswer) => <.div(^.className:="form-group", <.label(q.prompt), editBooleanA((q,a)))
        }
      )
    })
    .build

  val viewQuestionnaireAnswers = ReactComponentB[Seq[(Question,Answer[_])]]("viewQuestionnaireAnswers")
    .render_P ({ seq =>
    <.div(
      for (pair <- seq) yield pair match {
        case (q:ShortTextQuestion, a:ShortTextAnswer) => <.div(^.className:="form-group", <.label(q.prompt), viewShortTextA((q,a)))
        case (q:BooleanQuestion, a:BooleanAnswer) => <.div(^.className:="form-group", <.label(q.prompt), viewBooleanA((q,a)))
      }
    )
  }).build



}
