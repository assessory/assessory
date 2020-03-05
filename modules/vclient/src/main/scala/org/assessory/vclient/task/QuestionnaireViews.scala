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
      LatchRender(output) { to =>
        <.div(^.cls := "questionnaire",
          EditAnswers(task, to)
        )
      }
    )
  }



  case class EditAnswers(task:Task, var taskOutput:TaskOutput) extends VHtmlComponent {

    private var status = Latch.immediate(taskOutput)
    status.request
    var modified = false

    def qto = taskOutput.body match {
      case q:QuestionnaireTaskOutput => q
      case _ => QuestionnaireTaskOutput(Seq.empty) // FIXME: shouldn't be empty
    }

    val qt = task.body match {
      case q:QuestionnaireTask => q
      case _ => QuestionnaireTask(Seq.empty)
    }

    private val qmap = (for { q <- qt.questionnaire } yield q.id -> q).toMap

    def replaceAnswer(a:Answer, i:Int):Unit = {
      modified = true
      taskOutput = taskOutput.copy(body=qto.copy(answers = qto.answers.patch(i, Seq(a), 1)))
      rerender()
    }

    private def savable:Boolean = TaskOutputService.isUnsaved(taskOutput) || (status.isCompleted && modified)

    private def available:Boolean = taskOutput.finalised.nonEmpty

    private def notFinalisable:Option[String] = {
      if (taskOutput.finalised.nonEmpty) Some("Already published")
      else if (savable || taskOutput.id == TaskOutputService.invalidId) Some("Needs saving")
      else None
    }

    def save():Unit = {
      if (savable) {
        status = Latch.lazily {
          TaskOutputService.save(taskOutput).map(_.item)
        }

        status.request.onComplete {
          case Success(to) =>
            taskOutput = to
            modified = false
            rerender()
          case Failure(exception) =>
            println("failed")
            rerender()
        }


        rerender()
      }
    }

    def makeAvailable():Unit = {
      if (notFinalisable.isEmpty) {
        status = Latch.lazily {
          TaskOutputService.finalise(taskOutput).map(_.item)
        }

        status.request.onComplete {
          case Success(to) =>
            taskOutput = to
            modified = false
            rerender()
          case Failure(exception) =>
            println("failed")
            rerender()
        }

        rerender()
      }
    }

    private def saveButtons = {
      <.div(^.cls := "form-group",
        <.button(^.cls := "btn btn-default",
          ^.on("click") --> save(),
          ^.attr("disabled") ?= (if (savable) None else Some("disabled")),
          (if (savable) "Save" else if (!status.isCompleted) "(Saving)" else "Already saved")
        ),
        <.button(^.cls := "btn btn-primary",
          ^.on("click") --> makeAvailable(),
          ^.attr("disabled") ?= notFinalisable.map(_ => "disabled"),
          ("" + notFinalisable.getOrElse("Publish"))
        ),
        latchErrorRender(status)
      )
    }

    def render = <.div(
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
      saveButtons
    )

  }



}
