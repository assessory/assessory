package com.assessory.sjsreact.taskOutput

import com.assessory.api.question.{QuestionnaireTask, QuestionnaireTaskOutput}
import com.assessory.api.video.{YouTube, VideoTaskOutput}
import com.assessory.api.{TaskOutputBody, TargetUser, TaskOutput, Task}
import com.assessory.sjsreact
import com.assessory.sjsreact.{QuestionViews, CommonComponent }
import com.assessory.sjsreact.services._
import com.assessory.sjsreact.video.VideoViews.VTOState
import com.wbillingsley.handy.{Latch, Id}
import japgolly.scalajs.react._

import Id._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.util.Success

/**
  *
  */
object QuestionnaireTaskViews {

  /**
    * Default
    */
  val front = ReactComponentB[Task]("taskOutputEdit")
    .initialState_P(task => Latch.lazily{
      for {
        tos <- TaskOutputService.myOutputs(task.id).captureUserError
      } yield {
        val orig = tos.headOption
        val to = orig getOrElse TaskOutput(
          id = sjsreact.invalidId,
          task = task.id,
          by = TargetUser("self".asId),
          body = TaskOutputService.emptyBodyFor(task.body)
        )

        QTOState(task, orig, to, Latch.immediate(""))
      }
    })
    .renderBackend[QuestionnaireTaskOutputBackend]
    .build

  case class QTOState(task:Task, orig:Option[TaskOutput], to:TaskOutput, s:Latch[String])

  sealed trait Finalisable
  object IsFinalisable extends Finalisable
  object NeverSaved extends Finalisable
  object UnsavedChanges extends Finalisable
  object AlreadyFinalised extends Finalisable

  class QuestionnaireTaskOutputBackend($: BackendScope[_, Latch[QTOState]]) {

    def savable(vto:QTOState) = {
      !vto.orig.contains(vto.to) && vto.s.isCompleted
    }

    def finalisable(vto:QTOState) = {
      if (vto.to.finalised.nonEmpty)
        AlreadyFinalised
      else if (vto.to.id.id == "invalid")
        NeverSaved
      else if (savable(vto))
        UnsavedChanges
      else IsFinalisable
    }


    def update(body: TaskOutputBody):Callback = { $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latch.immediate(
            vto.copy(to = vto.to.copy(body = body))
          )
          case _ => latched
        }
      } else latched
    })}

    def save(finalise:Boolean = false) = $.modState({ latched =>
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
              } yield QTOState(vto.task, Some(wp.item), wp.item, Latch.immediate("Saved"))
              ) recover {
              case e:Exception => QTOState(vto.task, vto.orig, vto.to, Latch.immediate("Failed to save: " + e.getMessage))
            }
          )
        }
      } else latched
    })

    def finalise() = $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latch.lazily(
            (
              for {
                wp <- TaskOutputService.finalise(vto.to)
              } yield QTOState(vto.task, Some(wp.item), wp.item, Latch.immediate("Finalised"))
            ) recover {
              case e:Exception => QTOState(vto.task, vto.orig, vto.to, Latch.immediate("Failed to finalise: " + e.getMessage))
            }
          )
        }
      } else latched
    })

    def render(state:Latch[QTOState]) = {
      CommonComponent.latchR(state) { vto =>
        <.div(
          (vto.task.body, vto.to.body) match {
            case (q:QuestionnaireTask, qto:QuestionnaireTaskOutput) => QuestionViews.editQuestionnaireAs(vto.task, q, qto, update, () => save(false))
            case _ => <.div(
              "Not quite sure how to render that now"

            )
          },
          <.div(^.className := "form-group",
            <.button(^.className:="btn btn-primary", ^.disabled := !savable(vto), ^.onClick --> save(false), "Save"), " ",
            finalisable(vto) match {
              case IsFinalisable => <.button(^.className:="btn btn-default", ^.onClick --> finalise(), "Make available")
              case UnsavedChanges => <.button(^.className:="btn btn-default", ^.disabled := true, "(You have unsaved changes)")
              case NeverSaved => <.button(^.className:="btn btn-default", ^.disabled := true, "(Needs saving first)")
              case AlreadyFinalised => <.button(^.className:="btn btn-default", ^.disabled := true, "(Already available)")
            },
            <.div(^.cls := "text-info",
              CommonComponent.latchedString(vto.s)
            )
          )
        )

      }
    }
  }

}
