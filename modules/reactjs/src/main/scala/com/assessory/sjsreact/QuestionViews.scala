package com.assessory.sjsreact

import com.assessory.api.question._
import com.assessory.api.{TargetTaskOutput, Task, Target}
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



  val editQuestionnaireAnswers = ReactComponentB[Seq[(Question,Answer)]]("editQuestionnaireAnswers")
    .render_P ({ seq =>
      <.div(
        for (pair <- seq) yield pair match {
          case (q:ShortTextQuestion, a:ShortTextAnswer) => <.div(^.className:="form-group", <.label(q.prompt), editShortTextA((q,a)))
          case (q:BooleanQuestion, a:BooleanAnswer) => <.div(^.className:="form-group", <.label(q.prompt), editBooleanA((q,a)))
        }
      )
    })
    .build

  val viewQuestionnaireAnswers = ReactComponentB[Seq[(Question,Answer)]]("viewQuestionnaireAnswers")
    .render_P ({ seq =>
    <.div(
      for (pair <- seq) yield pair match {
        case (q:ShortTextQuestion, a:ShortTextAnswer) => <.div(^.className:="form-group", <.label(q.prompt), viewShortTextA((q,a)))
        case (q:BooleanQuestion, a:BooleanAnswer) => <.div(^.className:="form-group", <.label(q.prompt), viewBooleanA((q,a)))
      }
    )
  }).build




  def editBooleanA2(q:Question, a:BooleanAnswer, f: Answer => Callback) = {
    <.div(
        <.label(^.className := "radio-inline", <.input(^.`type` := "radio",
          ^.checked := a.answer.contains(true),
          ^.onChange ==> { (evt:ReactEventI) => f(a.copy(answer = Some(evt.target.checked))) } , "Yes")
        ),
        <.label(^.className := "radio-inline", <.input(^.`type` := "radio",
          ^.checked := a.answer == Some(false),
          ^.onChange ==> { (evt:ReactEventI) => f(a.copy(answer = Some(!evt.target.checked))) }, "No")
        )
      )
  }


  def editQuestionnaireAs(qt:QuestionnaireTask, qa:QuestionnaireTaskOutput, f: QuestionnaireTaskOutput => Callback) = {

    def subbing[T](i: Int, a: Answer) = {
      val patched = qa.answers.patch(i, Seq(a), 1)
      f(qa.copy(answers = patched))
    }

    <.div(
      for (pair <- qa.answers.zipWithIndex) yield pair match {
        case (a: ShortTextAnswer, i) =>
          <.div(^.className := "form-group", <.label("Haven't implemented Short Answers yet!"))
        case (a: BooleanAnswer, i) =>
          <.div(^.className := "form-group", <.label(qt.questionMap.apply(a.question).prompt), editBooleanA2(qt.questionMap(a.question), a, subbing(i, _)))
      }
    )

  }


}
