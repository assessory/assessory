package com.assessory.sjsreact.video

import com.assessory.api.question.{Answer, BooleanAnswer, Question}
import com.assessory.api.{TargetUser, TaskOutput, Task}
import com.assessory.api.client.EmailAndPassword
import com.assessory.api.video._
import com.assessory.sjsreact
import com.assessory.sjsreact.{CommonComponent, Front }
import com.assessory.sjsreact.services.{UserService, TaskOutputService}
import com.wbillingsley.handy.{Latch, Id}
import com.wbillingsley.handy.appbase.UserError
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import Id._


import scala.concurrent.Future
import scala.util.{Failure, Success}

object VideoViews {

  def video(url:String):Option[VideoResource] = {
    isYouTube(url)
      .orElse(isKaltura(url))
      .orElse({
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

  def isKaltura(url:String):Option[Kaltura] = {
    val withWWW="(https\\:\\/\\/kaf.une.edu.au\\/media\\/[^#\\&\\?\\/]*\\/)([^#\\&\\?]*).*".r
    val embed = ".*(playerId=kaltura_player\\&entry_id=)([^#\\&\\?]*).*".r

    withWWW.findFirstMatchIn(url).map(_.group(2))
      .orElse(embed.findFirstMatchIn(url).map(_.group(2)))
      .map({ case _ => Kaltura(url) })
  }

  def extractKalturaId(url:String):String = {
    val withWWW="(https\\:\\/\\/kaf.une.edu.au\\/media\\/[^#\\&\\?\\/]*\\/)([^#\\&\\?]*).*".r
    val embed = ".*(playerId=kaltura_player\\&entry_id=)([^#\\&\\?]*).*".r
    withWWW.findFirstMatchIn(url).map(_.group(2))
      .orElse(embed.findFirstMatchIn(url).map(_.group(2)))
      .getOrElse(url)
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

  def kalturaPlayer(url:String):ReactNode = {
    val extracted = extractKalturaId(url)

    /*
    * <iframe id="kaltura_player" src="https://cdnapisec.kaltura.com/p/424421/sp/42442100/embedIframeJs/uiconf_id/7033932/partner_id/424421?iframeembed=true&playerId=kaltura_player&entry_id=0_2qls91uv&flashvars[streamerType]=auto&amp;flashvars[localizationCode]=en&amp;flashvars[leadWithHTML5]=true&amp;flashvars[sideBarContainer.plugin]=true&amp;flashvars[sideBarContainer.position]=left&amp;flashvars[sideBarContainer.clickToClose]=true&amp;flashvars[chapters.plugin]=true&amp;flashvars[chapters.layout]=vertical&amp;flashvars[chapters.thumbnailRotator]=false&amp;flashvars[streamSelector.plugin]=true&amp;flashvars[EmbedPlayer.SpinnerTarget]=videoHolder&amp;flashvars[dualScreen.plugin]=true&amp;&wid=0_6d7thgve" width="480" height="270" allowfullscreen webkitallowfullscreen mozAllowFullScreen frameborder="0"></iframe>
    * */

    <.iframe(
      ^.width := "560", ^.height := "315",
      ^.src := s"https://cdnapisec.kaltura.com/p/424421/sp/42442100/embedIframeJs/uiconf_id/7033932/partner_id/424421?iframeembed=true&playerId=kaltura_player&entry_id=${extracted}", ^.frameBorder := "0", ^.allowFullScreen := true
    )
  }

  def player(vr:VideoResource):ReactNode = vr match {
    case YouTube(url) => youTubePlayer(url)
    case Kaltura(url)  => kalturaPlayer(url)
    case UnrecognisedVideoUrl(url) => <.a(^.href := url, ^.target := "_blank", "Unrecognised video URL: ", url)
  }

  def latchR[T](l:Latch[T])(render: T => ReactElement):ReactElement = {
    l.request.value match {
      case Some(Success(x)) => render(x)
      case Some(Failure(x)) => <.span(^.className := "error", x.getMessage)
      case _ => <.i(^.className := "fa fa-spinner fa-spin")
    }
  }

  case class VTOState(task:Task, orig:Option[TaskOutput], to:TaskOutput, s:Latch[String])

  class VideoTaskOutputBackend($: BackendScope[_, Latch[VTOState]]) {

    def savable(vto:VTOState) = {
      !vto.orig.contains(vto.to) && vto.s.isCompleted
    }

    def video(e: ReactEventI):Callback = { $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latch.immediate(
            vto.copy(to = vto.to.copy(body = VideoTaskOutput(Some(YouTube(e.target.value)))))
          )
          case _ => latched
        }
      } else latched
    })}

    def save = $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latch.lazily(
            (
              for {
                wp <- if (vto.orig.isDefined) {
                  TaskOutputService.updateBody(vto.to)
                } else {
                  TaskOutputService.createNew(vto.to)
                }
              } yield VTOState(vto.task, Some(wp.item), wp.item, Latch.immediate("Saved"))
            ) recover {
              case e:Exception => VTOState(vto.task, vto.orig, vto.to, Latch.immediate("Failed to save: " + e.getMessage))
            }
          )
        }
      } else latched
    })

    def render(state:Latch[VTOState]) = {
      latchR(state) { vto =>
        <.div(
          <.div(
            vto.orig.map(_.body) match {
              case Some(VideoTaskOutput(Some(vr))) => player(vr)
              case _ => <.div("No video submitted yet")
            }
          ),
          vto.to.body match {
            case VideoTaskOutput(Some(YouTube(ytId))) =>
              <.div(
                <.label("Video URL: "),
                <.input(^.`type` := "text", ^.onChange ==> video, ^.value := ytId)
              )
            case VideoTaskOutput(Some(Kaltura(ytId))) =>
              <.div(
                <.label("KVideo URL: "),
                <.input(^.`type` := "text", ^.onChange ==> video, ^.value := ytId)
              )
            case VideoTaskOutput(Some(UnrecognisedVideoUrl(ytId))) =>
              <.div(
                <.label("Video URL: "),
                <.input(^.`type` := "text", ^.onChange ==> video, ^.value := ytId)
              )
            case VideoTaskOutput(None) =>
              <.div(
                <.label("Video URL: "),
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
    .initialState_P(task => Latch.lazily{
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

        VTOState(task, orig, to, Latch.immediate(""))
      }
    })
    .renderBackend[VideoTaskOutputBackend]
    .build


}
