package com.assessory.sjsreact.video

import com.assessory.api.question.{Answer, BooleanAnswer, Question}
import com.assessory.api.{TargetUser, TaskOutput, Task}
import com.assessory.api.client.EmailAndPassword
import com.assessory.api.video._
import com.assessory.sjsreact
import com.assessory.sjsreact.{CommonComponent, Front, Latched}
import com.assessory.sjsreact.services.{UserService, TaskOutputService}
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.UserError
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactEventI, ReactElement, BackendScope, ReactComponentB, Callback}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import Id._


import scala.concurrent.Future
import scala.util.{Failure, Success}

object VideoViews {

  def video(url:String):Option[VideoResource] = {
    isYouTube(url).orElse({
      if (url.trim.isEmpty) None else Some(UnrecognisedVideoUrl(url))
    })
  }

  /*
   * If this is a YouTube URL, returns Some(url), else None
   */
  def isYouTube(url:String):Option[YouTube] = {
    val withWWW = "(https\\:\\/\\/www.youtube.com\\/watch\\?v=)([^#\\&\\?]*).*".r
    val youTudotBe = ".*(youtu.be\\/)([^#\\&\\?]*).*".r
    withWWW.findFirstMatchIn(url).map(_.group(2))
      .orElse(youTudotBe.findFirstMatchIn(url).map(_.group(2))).map({ case _ => YouTube(url) })
  }

  def extractYouTubeId(string:String) = {
    val withWWW = "(https\\:\\/\\/www.youtube.com\\/watch\\?v=)([^#\\&\\?]*).*".r
    val youTudotBe = ".*(youtu.be\\/)([^#\\&\\?]*).*".r
    withWWW.findFirstMatchIn(string).map(_.group(2))
      .orElse(youTudotBe.findFirstMatchIn(string).map(_.group(2)))
      .getOrElse(string)
  }

  def youTubePlayer(ytId:String) = {
    val extracted = extractYouTubeId(ytId)

    <.iframe(
      ^.width := "560", ^.height := "315",
      ^.src := s"https://www.youtube.com/embed/${extracted}", ^.frameBorder := "0", ^.allowFullScreen := true
    )
  }

  def latchR[T](l:Latched[T])(render: T => ReactElement):ReactElement = {
    l.request.value match {
      case Some(Success(x)) => render(x)
      case Some(Failure(x)) => <.span(^.className := "error", x.getMessage)
      case _ => <.i(^.className := "fa fa-spinner fa-spin")
    }
  }

  case class VTOState(task:Task, orig:Option[TaskOutput], to:TaskOutput, s:Latched[String])

  class VideoTaskOutputBackend($: BackendScope[_, Latched[VTOState]]) {

    def savable(vto:VTOState) = {
      !vto.orig.contains(vto.to) && vto.s.isCompleted
    }

    def video(e: ReactEventI):Callback = { $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latched.immediate(
            vto.copy(to = vto.to.copy(body = VideoTaskOutput(Some(YouTube(e.target.value)))))
          )
          case _ => latched
        }
      } else latched
    })}

    def save = $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latched.lazily(
            (
              for {
                wp <- if (vto.orig.isDefined) {
                  TaskOutputService.updateBody(vto.to)
                } else {
                  TaskOutputService.createNew(vto.to)
                }
              } yield VTOState(vto.task, Some(wp.item), wp.item, Latched.immediate("Saved"))
            ) recover {
              case e:Exception => VTOState(vto.task, vto.orig, vto.to, Latched.immediate("Failed to save: " + e.getMessage))
            }
          )
        }
      } else latched
    })

    def render(state:Latched[VTOState]) = {
      latchR(state) { vto =>
        <.div(
          <.div(
            vto.orig.map(_.body) match {
              case Some(VideoTaskOutput(Some(YouTube(ytId)))) =>
                youTubePlayer(extractYouTubeId(ytId))
              case _ => <.div("No video submitted yet")

            }
          ),
          vto.to.body match {
            case VideoTaskOutput(Some(YouTube(ytId))) =>
              <.div(
                <.label("YouTube video share URL or video ID "),
                <.input(^.`type` := "text", ^.onChange ==> video, ^.value := ytId)
              )
            case VideoTaskOutput(None) =>
              <.div(
                <.label("YouTube video share URL or video ID "),
                <.input(^.`type` := "text", ^.onChange ==> video)
              )
            case _ => <.div("Hang on, this reckons you're answering this as something other than a video?")
          },
          <.div(^.className := "form-group",
            <.button(^.className:="btn btn-primary", ^.disabled := !savable(vto), ^.onClick --> save, "Save"),
            CommonComponent.latchedString(vto.s)
          )
        )
      }
    }
  }

  val front = ReactComponentB[Task]("critiqueTaskView")
    .initialState_P(task => Latched.lazily{
      for {
        tos <- TaskOutputService.myOutputs(task.id)
      } yield {
        val orig = tos.headOption
        val to = orig getOrElse TaskOutput(
          id = sjsreact.invalidId,
          task = task.id,
          by = TargetUser("self".asId),
          body = VideoTaskOutput(None)
        )

        VTOState(task, orig, to, Latched.immediate(""))
      }
    })
    .renderBackend[VideoTaskOutputBackend]
    .build


}
