package org.assessory.vclient.task

import com.assessory.api.question.{Answer, Question, QuestionnaireTask, QuestionnaireTaskOutput, ShortTextAnswer, VideoAnswer}
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
            case v:VideoAnswer => VideoQViews.editVideoAnswer(qmap(a.question), v) { a => replaceAnswer(a, i) }
            case s:ShortTextAnswer => ShortTextQViews.editShortTextAnswer(qmap(a.question), s) { s => replaceAnswer(s, i) }
            case _ => <.div(s"Missing edit renderer for ${a.getClass.getName}")
          }

        )
      }
    )

  }



}
