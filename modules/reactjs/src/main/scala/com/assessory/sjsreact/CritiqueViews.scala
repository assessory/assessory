package com.assessory.sjsreact

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{CritiqueTask, CritAllocation, Critique}
import com.assessory.api.question.{QuestionnaireTask, QuestionnaireTaskOutput}
import com.assessory.api.video.{YouTube, VideoTaskOutput}
import com.assessory.sjsreact
import com.assessory.sjsreact.services.{TaskService, TaskOutputService}
import com.assessory.sjsreact.video.VideoViews
import com.assessory.sjsreact.video.VideoViews.VTOState
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object CritiqueViews {

  case class Selection[T,C](private var _selected: Option[T], seq:Seq[T], context:C) {
    def selected_=(o:Option[T]) = {
      this._selected = o
      WebApp.rerender()
    }

    def selected = _selected
  }

  case class Saveable[T,R](item: T, l:Latched[R])


  val allocationsSwitch = ReactComponentB[Selection[Target,Task]]("critAllocationSelection")
    .render_P { sel =>
       <.ul(^.className := "nav nav-pills", ^.role := "group",
         for ((targ, idx) <- sel.seq.zipWithIndex) yield {
           <.li(^.className := (if (sel.selected == Some(targ)) "active" else ""), ^.role := "presentation",
             <.a(^.onClick --> Callback { sel.selected = Some(targ) },
               targ match {
                 case TargetGroup(g) => GroupViews.groupNameId(g)
                 case TargetTaskOutput(to) => idx
               }
             )
           )
         }
       )
    }
    .build

  val reviewTO = CommonComponent.latchedRender[(Task,TaskOutput)]("reviewOutput") { case (task, taskOutput) =>
    (task.body, taskOutput.body) match {
      case (ct:CritiqueTask, c:Critique) =>

        <.div(^.className := "panel",
          <.h3("Their responses"),
          <.div(
          )
        )
    }
  }

  val reviewTarget = ReactComponentB[Selection[Target,Task]]("reviewTarget")
    .render_P { sel =>
      sel.selected match {
        case Some(TargetTaskOutput(to)) =>
          reviewTO(Latched.eagerly {
            for {
              taskOutput <- TaskOutputService.latch(to).request
              task <- TaskService.latch(taskOutput.item.task).request
            } yield (task.item, taskOutput.item)
          })
        case _ => <.div()
      }
    }
    .build



  case class CompleteCritProps(task:Task, wpTo:WithPerms[TaskOutput])
  case class CompleteCritState(task:Task, wpTo:WithPerms[TaskOutput], answers:Seq[Answer[_]], message:Latched[String])

  class CompleteCritBackend($: BackendScope[CompleteCritProps, CompleteCritState]) {

    def render(props:CompleteCritProps, state:CompleteCritState) =
      <.div(
        props.task.body match {
          case _ => <.div("Unexpected content - didn't seem to be a critique")
        },
        <.button(^.className := "btn btn-primary ", ^.disabled := unchanged(state), ^.onClick --> save(), "Save"),
        status(state.message)
      )


    def save() = $.modState { state => state /*
      state.wpTo.item.body match {
        case crit:Critique => {
          val toSave = state.wpTo.item.copy(body = crit.copy(answers = state.answers))
          val f = TaskOutputService.updateBody(toSave)
          val newLatch = Latched.lazily(f.map(_ => ""))
          CompleteCritState(state.task, state.wpTo, state.answers, newLatch)
        }
      } */
    }

    def unchanged(s:CompleteCritState): Boolean = {
      s.wpTo.item.body match {
        case crit: Critique => crit.task == s.answers
      }
    }

  }

  def copyAns(a:Answer[_]):Answer[_] = a match {
    case a:ShortTextAnswer => a.copy()
    case a:BooleanAnswer => a.copy()
  }


  val status = CommonComponent.latchedRender[String]("status") { str => <.span(str) }

  val completeCrit = CommonComponent.latchedX[CompleteCritProps]("completeCrit") { comp =>
    comp.initialState_P { props => props.wpTo.item.body match {
      case c:Critique => CompleteCritState(props.task, props.wpTo, Seq.empty, Latched.immediate(""))
    }}
    .renderBackend[CompleteCritBackend]
    .build
  }


  val critFormTargF = CommonComponent.latchedRender[(Task, Id[TaskOutput, String])]("critFormTarg") {
    case (task, id) =>
      val fPair = TaskOutputService.future(id).map(CompleteCritProps(task, _))
      completeCrit(Latched.lazily(fPair))
  }


  case class TargetAndTask(target:Target, task:Task)
  case class CritFormTargState(target:Target, task:Task, toId:Future[Id[TaskOutput, String]])

  /**
   * Find and load the critique for this target
   */
  val critFormTarg = ReactComponentB[TargetAndTask]("critFormTarg")
    .initialState_P { p => CritFormTargState(p.target, p.task, TaskOutputService.findOrCreateCrit(p.task.id, p.target).map(_.id)) }
    .render_S { state =>
      val lpair = for {
        outputId <- state.toId
      } yield (state.task, outputId)

      critFormTargF(Latched.lazily(lpair))
    }
    .build



  val frontInt = CommonComponent.latchedRender[Selection[Target,Task]]("critiqueTaskViewInt") { sel =>
    <.div(
      allocationsSwitch(sel),
      reviewTarget(sel),
      sel.selected match {
        case Some(t) => critFormTarg(TargetAndTask(t, sel.context))
        case _ => <.div()
      }
    )
  }

  val front = ReactComponentB[Task]("critiqueTaskView")
    .initialState_P(task => Latched.lazily{
      for (alloc <- TaskOutputService.myAllocations(task.id)) yield new Selection(None, alloc, task)
    })
    .render_S(c => frontInt(c))
    .build

  def futureRender[T](name:String)(render: T => ReactElement) = {
    val inner = ReactComponentB[T](name)
      .render_P(render)
      .build

    ReactComponentB[Future[T]]("Future"+name)
      .render_P({ f:Future[T] =>
      f.value match {
        case Some(Success(x)) => inner(x)
        case Some(Failure(x)) => <.span(^.className := "error", x.getMessage)
        case _ => <.i(^.className := "fa fa-spinner fa-spin")
      }
    })
      .build
  }


  //
  ///
  //
  ///
  //





  def allocationsSwitchTwo[A,B](sel:Selection[A,B]) = {
    <.ul(^.className := "nav nav-pills", ^.role := "group",
      for ((targ, idx) <- sel.seq.zipWithIndex) yield {
        <.li(^.className := (if (sel.selected == Some(targ)) "active" else ""), ^.role := "presentation",
          <.a(^.onClick --> Callback { sel.selected = Some(targ) },
            idx + 1
          )
        )
      }
    )
  }


  /**
    * Renders a critique tartget
    */
  def renderCritTarget(t:Target):ReactNode = t match {
    // TODO: Implement render targets for other kinds of target
    case TargetTaskOutput(toId) => TaskViews.preview(toId)
    case TargetGroup(gId) => GroupViews.groupNameId(gId)
    case _ => <.div(^.cls := "alert alert-error", "Unrenderable target")
  }

  case class CritBackendState(task:Task, orig:Option[TaskOutput], to:TaskOutput, s:Latched[String])

  sealed trait Finalisable
  object IsFinalisable extends Finalisable
  object UnsavedChanges extends Finalisable
  object NeverSaved extends Finalisable
  object AlreadyFinalised extends Finalisable

  class CritBackend($: BackendScope[_, Latched[CritBackendState]]) {

    def savable(vto:CritBackendState) = {
      !vto.orig.contains(vto.to) && vto.s.isCompleted
    }

    def finalisable(vto:CritBackendState) = {
      if (vto.to.finalised.nonEmpty)
        AlreadyFinalised
      else if (vto.to.id.id == "invalid")
        NeverSaved
      else if (savable(vto))
        UnsavedChanges
      else IsFinalisable
    }

    def renderTarget(task:Task, tob:TaskOutputBody):ReactNode = tob match {
      // TODO: Implement render targets for other kinds of target
      case Critique(TargetTaskOutput(toId), _) => TaskViews.preview(toId)
      case Critique(TargetGroup(gId), _) => GroupViews.groupNameId(gId)
      case _ => <.div(^.cls := "alert alert-error", "Unrenderable target")
    }

    def renderCrit(tob:TaskOutputBody):ReactElement = tob match {
      case Critique(_, VideoTaskOutput(v)) => <.div("video")
    }

    def save:Callback = $.modState({ latched =>
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
              } yield CritBackendState(vto.task, Some(wp.item), wp.item, Latched.immediate("Saved"))
              ) recover {
              case e:Exception => CritBackendState(vto.task, vto.orig, vto.to, Latched.immediate("Failed to save: " + e.getMessage))
            }
          )
        }
      } else latched
    })

    def finalise():Callback = $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latched.lazily(
            (
              for {
                wp <- TaskOutputService.finalise(vto.to)
              } yield CritBackendState(vto.task, Some(wp.item), wp.item, Latched.immediate("Finalised"))
              ) recover {
              case e:Exception => CritBackendState(vto.task, vto.orig, vto.to, Latched.immediate("Failed to save: " + e.getMessage))
            }
          )
        }
      } else latched
    })

    def stateFromProps(p:(Task,Id[TaskOutput, String])) = $.modState { s => s.request.value match {
      case Some(Success(CritBackendState(_, _, TaskOutput(id, _, _, _, _, _, _, _), _))) if id == p._2 => s
      case _ => Latched.lazily(
        for {
          to <- TaskOutputService.future(p._2)
        } yield CritBackendState(p._1, Some(to.item), to.item, Latched.immediate(""))
      )
    }}


    def video(e: ReactEventI):Callback = { $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latched.immediate(
            vto.copy(to = vto.to.copy(body = vto.to.body match {
              case Critique(targ, VideoTaskOutput(_)) => Critique(targ, VideoTaskOutput(Some(YouTube(e.target.value))))
            }))
          )
          case _ => latched
        }
      } else latched
    })}

    def body(qto:QuestionnaireTaskOutput):Callback = { $.modState({ latched =>
      if (latched.isCompleted) {
        latched.request.value match {
          case Some(Success(vto)) => Latched.immediate(
            vto.copy(to = vto.to.copy(body = vto.to.body match {
              case Critique(targ, _) => Critique(targ, qto)
            }))
          )
          case _ => latched
        }
      } else latched
    })}


    def render(lState:Latched[CritBackendState]) = CommonComponent.latchR(lState) { state =>
      <.div(
        <.div(^.cls := "panel panel-default",
          <.div(^.cls := "panel-heading", "What you are critiquing:"),
          <.div(^.cls := "panel-body",
            renderTarget(state.task, state.to.body)
          )
        ),

        <.h3("Your critique"),
        state.orig.map(_.body) match {
          case Some(Critique(_, VideoTaskOutput(Some(YouTube(ytId))))) => VideoViews.youTubePlayer(ytId)
          case Some(Critique(_, VideoTaskOutput(None))) => <.div("No video submitted yet")
          case _ => <.div("")
        },

        (state.task.body, state.to.body) match {
          case (_, Critique(_, VideoTaskOutput(Some(YouTube(ytId))))) =>
            <.div(
              <.label("YouTube video share URL or video ID "),
              <.input(^.`type` := "text", ^.onChange ==> video, ^.value := ytId)
            )
          case (_, Critique(_, VideoTaskOutput(None))) =>
            <.div(
              <.label("YouTube video share URL or video ID "),
              <.input(^.`type` := "text", ^.onChange ==> video)
            )
          case (CritiqueTask(_, qt:QuestionnaireTask), Critique(_, qa:QuestionnaireTaskOutput)) =>
            <.div(
              QuestionViews.editQuestionnaireAs(state.task, qt, qa, body, () => save)
            )
          case _ => <.div("Hang on, this reckons you're answering this as something other than a video?")
        },

        <.div(^.className := "form-group",
          <.button(^.className:="btn btn-primary", ^.disabled := !savable(state), ^.onClick --> save, "Save"), " ",
          finalisable(state) match {
            case IsFinalisable => <.button(^.className:="btn btn-default", ^.onClick --> finalise(), "Finalise")
            case UnsavedChanges => <.button(^.className:="btn btn-default", ^.disabled := true, "(You have unsaved changes)")
            case NeverSaved => <.button(^.className:="btn btn-default", ^.disabled := true, "(Needs saving first)")
            case AlreadyFinalised => <.button(^.className:="btn btn-default", ^.disabled := true, "(Already finalised)")
          },
          <.div(^.cls := "text-info",
            CommonComponent.latchedString(state.s)
          )
        )
      )
    }

  }

  val renderCrit = ReactComponentB[(Task, Id[TaskOutput, String])]("renderCritOutput")
    .initialState_P({ p => Latched.lazily(
      for {
        to <- TaskOutputService.future(p._2)
      } yield CritBackendState(p._1, Some(to.item), to.item, Latched.immediate(""))
    )})
    .renderBackend[CritBackend]
    .componentWillReceiveProps({ x => x.$.backend.stateFromProps(x.nextProps) })
    .build

  val frontIntTwo = ReactComponentB[Selection[Id[TaskOutput, String],Task]]("critAllocationSelection")
    .render_P({ sel =>
      <.div(
        allocationsSwitchTwo(sel),
        sel.selected match {
          case Some(target) => renderCrit((sel.context, target))
          case _ => <.div()
        }
      )
    })
    .build


  val frontTwo = ReactComponentB[Task]("critiqueTaskView")
    .initialState_P(task => Latched.lazily{
      for {
        alloc <- TaskOutputService.taskOutputsFor(task.id)

        ids = alloc.map(_.id)
      } yield new Selection(None, ids, task)
    })
    .render_S(c => CommonComponent.latchR(c) { sel =>
      <.div(
        allocationsSwitchTwo(sel),
        sel.selected match {
          case Some(target) => renderCrit((sel.context, target))
          case _ => <.div()
        }
      )

    })
    .build



}
