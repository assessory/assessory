package com.assessory.sjsreact

import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{CritiqueTask, CritAllocation, Critique}
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
    .initialState_P { p => CritFormTargState(p.target, p.task, TaskOutputService.findOrCreateCrit(p.task.id, p.target)) }
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


  def latchR[T](l:Latched[T])(render: T => ReactElement):ReactElement = {
    l.request.value match {
      case Some(Success(x)) => render(x)
      case Some(Failure(x)) => <.span(^.className := "error", x.getMessage)
      case _ => <.i(^.className := "fa fa-spinner fa-spin")
    }
  }





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


  val frontIntTwo = ReactComponentB[Selection[TaskOutput,Task]]("critAllocationSelection")
    .render_P({ sel =>
      //val targets = sel.seq.map { case TaskOutput(_, _, _, _, Critique(targ, _), _, _, _) => targ }

      <.div(
        allocationsSwitchTwo(sel),
        sel.selected match {
          case Some(TaskOutput(_, _, _, _, Critique(TargetTaskOutput(toId), _), _, _, _)) =>
            latchR(TaskOutputService.latch(toId)) { wp => wp.item.body match {
              case VideoTaskOutput(Some(YouTube(ytId))) =>
                <.div(
                  VideoViews.youTubePlayer(ytId),
                  <.div(^.cls := "alert alert-warning", "Form to submit your video will go up tomorrow. Check back here then")
                )
              case _ => <.div(
                <.div("Unrenderable content"),
                <.div(^.cls := "alert alert-warning", "Form to submit your critique video will go up tomorrow. Check back here then")
              )
            }}
          case _ => <.div()
        }
      )
    })
    .build


  val frontTwo = ReactComponentB[Task]("critiqueTaskView")
    .initialState_P(task => Latched.lazily{
      for {
        alloc <- TaskOutputService.taskOutputsFor(task.id)
      } yield new Selection(None, alloc, task)
    })
    .render_S(c => latchR(c) { frontIntTwo(_) })
    .build



}
