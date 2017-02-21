package com.assessory.sjsreact

import com.assessory.api.question._
import com.assessory.api.video._
import com.assessory.api.{TargetTaskOutput, Task, Target}
import com.assessory.sjsreact.files.FileViews
import com.assessory.sjsreact.files.FileViews.SmallFileUploadProps
import com.assessory.sjsreact.services.FileService
import com.assessory.sjsreact.video.VideoViews
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.Course
import japgolly.scalajs.react.{ReactNode, Callback, ReactEventI, ReactComponentB}
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
        case (q:VideoQuestion, a:VideoAnswer) => <.div(viewVideoAnswer(q, a))
        case (q:FileQuestion, a:FileAnswer) => <.div(viewFileAnswer(q, a))
        case x => <.div("could not render", x.toString)
      }
    )
  }).build


  def editShortTextA2(q:Question, a:ShortTextAnswer, f: Answer => Callback) = {
    <.div(
      <.div(
        <.textarea(^.className := "form-control", ^.value := (a.answer.getOrElse(""):String),
          ^.onChange ==> { (evt:ReactEventI) => f(a.copy(answer = Some(evt.target.value))) }
        )
      )
    )
  }

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


  def viewVideoAnswer(q:VideoQuestion, a:VideoAnswer):ReactNode = {
    a.answer match {
      case Some(vr) =>
        VideoViews.player(vr)
      case _ => <.div("No video submitted yet")
    }
  }

  def editVideoAnswer(q:Question, a:VideoAnswer, f: Answer => Callback) = {

    def updateVideo(url:String):Callback = {
      f(a.copy(answer=VideoViews.video(url)))
    }

    <.div(
      <.div(
        a.answer match {
          case Some(vr) =>
            VideoViews.player(vr)
          case _ => <.div("No video submitted yet")
        }
      ),
      a.answer match {
        case Some(YouTube(ytId)) =>
          <.div(
            <.label("Video URL "),
            <.input(^.`type` := "text", ^.value := ytId,
              ^.onChange ==> { (evt:ReactEventI) => updateVideo(evt.target.value) }
            )
          )
        case Some(Kaltura(ytId)) =>
          <.div(
            <.label("Video URL "),
            <.input(^.`type` := "text", ^.value := ytId,
              ^.onChange ==> { (evt:ReactEventI) => updateVideo(evt.target.value) }
            )
          )
        case Some(UnrecognisedVideoUrl(url)) =>
          <.div(
            <.label("Video URL "),
            <.input(^.`type` := "text", ^.value := url,
              ^.onChange ==> { (evt:ReactEventI) => updateVideo(evt.target.value) }
            )
          )
        case _ =>
          <.div(
            <.label("Video URL"),
            <.input(^.`type` := "text",
              ^.onChange ==> { (evt:ReactEventI) => updateVideo(evt.target.value) }
            )
          )
        case _ => <.div("Hang on, this reckons you're answering this as something other than a video?")
      }
    )
  }

  def viewFileAnswer(q:FileQuestion, a:FileAnswer):ReactNode = {
    a.answer match {
      case Some(f) => FileViews.fileLink(f)
      case None => <.div("No file uploaded")
    }
  }

  def editFileAnswer(c:Id[Course, String], q:Question, a:FileAnswer, f: Answer => Callback, autosave: () => Callback) = {
    <.div(
      FileViews.smallFileUploadWidget(SmallFileUploadProps(
        c,
        a.answer,
        (oId:Option[Id[SmallFile, String]]) => {
          f(a.copy(answer = oId)).runNow()
          autosave().runNow()
        }
      ))
    )
  }



  def editQuestionnaireAs(task:Task, qt:QuestionnaireTask, qa:QuestionnaireTaskOutput, f: QuestionnaireTaskOutput => Callback, autosave: () => Callback) = {

    def subbing[T](i: Int, a: Answer) = {
      val patched = qa.answers.patch(i, Seq(a), 1)
      f(qa.copy(answers = patched))
    }

    <.div(
      for (pair <- qa.answers.zipWithIndex) yield pair match {
        case (a: ShortTextAnswer, i) =>
          <.div(^.className := "form-group", <.label(qt.questionMap.apply(a.question).prompt), editShortTextA2(qt.questionMap(a.question), a, subbing(i, _)))
        case (a: BooleanAnswer, i) =>
          <.div(^.className := "form-group", <.label(qt.questionMap.apply(a.question).prompt), editBooleanA2(qt.questionMap(a.question), a, subbing(i, _)))
        case (a: VideoAnswer, i) =>
          <.div(^.className := "form-group", <.label(qt.questionMap.apply(a.question).prompt), editVideoAnswer(qt.questionMap(a.question), a, subbing(i, _)))
        case (a: FileAnswer, i) =>
          <.div(^.className := "form-group", <.label(qt.questionMap.apply(a.question).prompt), editFileAnswer(task.course, qt.questionMap(a.question), a, subbing(i, _), autosave))

        case _ => <.div("No view yet for " + pair._1)
      }
    )

  }


}
