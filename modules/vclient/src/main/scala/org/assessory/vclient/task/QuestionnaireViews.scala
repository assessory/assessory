package org.assessory.vclient.task

import com.assessory.api.question.{Answer, Question, QuestionnaireTask, QuestionnaireTaskOutput, ShortTextAnswer, VideoAnswer}
import com.assessory.api.video.{Kaltura, UnrecognisedVideoUrl, VideoResource, YouTube}
import com.assessory.api.{TargetUser, Task, TaskOutput}
import com.wbillingsley.handy.{Id, Latch}
import com.wbillingsley.veautiful.html.{<, MarkupNode, VHtmlComponent, VHtmlNode, ^}
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.services.TaskOutputService
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import org.assessory.vclient.services._
import org.assessory.vclient.common.Markup
import org.assessory.vclient.common.Components._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

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

    var output = Latch.lazily(loadOrDefaultOutput(task))

    def render = <.div(
      LatchRender(output) { to => <.div(TaskViews.EditOutputBody(task, to)) }
    )
  }



  def editAnswers(q:QuestionnaireTask, qto:QuestionnaireTaskOutput)(update: QuestionnaireTaskOutput => Unit, actions: => Seq[VHtmlNode]):VHtmlNode = {

    val qmap = (for { q <- q.questionnaire} yield q.id -> q).toMap

    def replaceAnswer(a:Answer, i:Int):Unit = {
      update(qto.copy(answers = qto.answers.patch(i, Seq(a), 1)))
    }

    <.div(
      for { (a, i) <- qto.answers.zipWithIndex } yield {
        <.div(^.cls := "question",
          <.div(
            Markup.marked.MarkupNode(() => qmap(a.question).prompt)
          ),
          a match {
            case v:VideoAnswer => VideoQViews.editVideoAnswer(qmap(a.question), v) { a => replaceAnswer(a, i) }
            case s:ShortTextAnswer => ShortTextQViews.editShortTextAnswer(qmap(a.question), s) { s => replaceAnswer(s, i) }
            case _ => <.div(s"Missing edit renderer for ${a.getClass.getName}")
          }

        )
      },
      <.div(^.cls := "form-group", actions)
    )
  }

  def previewAnswers(q:QuestionnaireTask, qto:QuestionnaireTaskOutput):VHtmlNode = {

    val qmap = (for { q <- q.questionnaire} yield q.id -> q).toMap

    <.div(
      for { (a, i) <- qto.answers.zipWithIndex } yield {
        <.div(^.cls := "question",
          <.div(
            Markup.marked.MarkupNode(() => qmap(a.question).prompt)
          ),
          a match {
            case v:VideoAnswer => VideoQViews.viewVideoAnswer(qmap(a.question), v)
            case s:ShortTextAnswer => ShortTextQViews.viewShortTextAnswer(qmap(a.question), s)
            case _ => <.div(s"Missing view renderer for ${a.getClass.getName}")
          }

        )
      }
    )
  }



}
