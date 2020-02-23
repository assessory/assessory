package org.assessory.vclient.task

import com.assessory.api.question.{Question, VideoAnswer}
import com.assessory.api.video.{Kaltura, UnrecognisedVideoUrl, VideoResource, YouTube}
import com.wbillingsley.veautiful.html.{<, VHtmlNode, ^}
import org.assessory.vclient.services.VideoService

import org.assessory.vclient.common.Components._

/**
 * Views for rendering Video Questions
 */
object VideoQViews {

  /** A player for YouTube videos */
  def youTubePlayer(ytId:String):VHtmlNode = {
    val extracted = VideoService.extractYouTubeId(ytId)

    <("iframe")(
      ^.attr("width") := "560", ^.attr("height") := "315",
      ^.src := s"https://www.youtube.com/embed/${extracted}", ^.attr("frameBorder") := "0", ^.attr("allowFullScreen") := "true"
    )
  }

  /** A player for Kaltura videos */
  def kalturaPlayer(url:String):VHtmlNode = {
    val extracted = VideoService.extractKalturaId(url)

    <("iframe")(
      ^.attr("width") := "560", ^.attr("height") := "315",
      ^.src := s"https://cdnapisec.kaltura.com/p/424421/sp/42442100/embedIframeJs/uiconf_id/7033932/partner_id/424421?iframeembed=true&playerId=kaltura_player&entry_id=${extracted}", ^.attr("frameBorder") := "0", ^.attr("allowFullScreen") := "true"
    )
  }

  def videoPlayer(vr:VideoResource):VHtmlNode = vr match {
    case YouTube(url) => youTubePlayer(url)
    case Kaltura(url)  => kalturaPlayer(url)
    case UnrecognisedVideoUrl(url) => <.a(^.href := url, ^.attr("target") := "_blank", "Unrecognised video URL: ", url)
  }

  def editVideoAnswer(q:Question, a:VideoAnswer)(f: VideoAnswer => Unit):VHtmlNode = {
    def updateVideo(url:String):Unit = {
      f(a.copy(answer=VideoService.video(url)))
    }

    <.div(^.cls := "form-group",
      a.answer match {
        case Some(vr) => videoPlayer(vr)
        case _ => <.div("No video chosen yet")
      },
      <.div(^.cls := "input-group",
        <.input(^.cls := "form-control col-md-6", ^.attr("type") := "text",
          ^.attr("placeholder") := "Paste video URL or embed code to replace",
          ^.on("change") ==> { e => e.inputValue.foreach(updateVideo) }
        ),
        <.div(^.cls := "input-group-append", <.button(^.cls := "btn btn-secondary", "Preview"))
      )
    )
  }

}
