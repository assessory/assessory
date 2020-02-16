package org.assessory.vclient.task

import com.assessory.api.question.{Answer, Question, QuestionnaireTask, QuestionnaireTaskOutput, VideoAnswer}
import com.assessory.api.video.{Kaltura, UnrecognisedVideoUrl, VideoResource, YouTube}
import com.assessory.api.{TargetUser, Task, TaskOutput}
import com.wbillingsley.handy.{Id, Latch}
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode, ^}
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.services.TaskOutputService
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import org.assessory.vclient.services._
import org.assessory.vclient.common.Components._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuestionnaireViews {

  def loadOrDefaultOutput(task:Task):Future[TaskOutput] = {
    for { outputs <- TaskOutputService.myOutputs(task.id) } yield {
      outputs.headOption.getOrElse(TaskOutputService.blankOutputFor(task, TargetUser("self".asId)))
    }
  }


  /**
   * Edit view for questionnaire tasks
   */
  case class EditOutputView(task:Task) extends VHtmlComponent {

    val output = Latch.lazily(loadOrDefaultOutput(task))

    def render = <.div(
      LatchRender(output) { to =>
        <.div(^.cls := "questionnaire",
          EditAnswers(task, to)
        )
      }
    )
  }

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

    <.div(
      a.answer match {
        case Some(vr) => videoPlayer(vr)
        case _ => <.div("No video chosen yet")
      },
      <.div(
        <("label")("Paste video URL or embed code to replace: "),

        <.input(^.attr("type") := "text", ^.on("change") ==> { e => e.inputValue.foreach(updateVideo) }),
        <.button("Set")
      )
    )
  }

  case class EditAnswers(task:Task, var original:TaskOutput) extends VHtmlComponent {

    var qto = original.body match {
      case q:QuestionnaireTaskOutput => q
      case _ => QuestionnaireTaskOutput(Seq.empty) // FIXME: shouldn't be empty
    }

    val qt = task.body match {
      case q:QuestionnaireTask => q
      case _ => QuestionnaireTask(Seq.empty)
    }

    private val qmap = (for { q <- qt.questionnaire } yield q.id -> q).toMap

    def replaceAnswer(a:Answer, i:Int):Unit = {
      qto = qto.copy(answers = qto.answers.patch(i, Seq(a), 1))
      rerender()
    }

    def render = <.div(
      for { (a, i) <- qto.answers.zipWithIndex } yield {
        <.div(^.cls := "form-group", <("label")(qmap(a.question).prompt),
          a match {
            case v:VideoAnswer => editVideoAnswer(qmap(a.question), v) { a => replaceAnswer(a, i) }
            case _ => <.div(s"Missing edit renderer for ${a.getClass.getName}")
          }

        )
      }
    )

  }



}
