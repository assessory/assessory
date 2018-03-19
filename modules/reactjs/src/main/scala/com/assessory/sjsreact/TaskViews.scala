package com.assessory.sjsreact

import com.assessory.api._
import com.assessory.api.question.{QuestionnaireTask, ShortTextAnswer, QuestionnaireTaskOutput}
import com.assessory.api.video.{YouTube, VideoTaskOutput, VideoTask}
import com.assessory.sjsreact.taskOutput.QuestionnaireTaskViews
import com.assessory.sjsreact.video.VideoViews
import due._
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{AllocateStrategy, CritiqueTask, Critique}
import com.assessory.sjsreact.services.{TaskOutputService, CourseService, TaskService, GroupService}
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.Ids._
import com.wbillingsley.handy.appbase.{Group, Course}
import japgolly.scalajs.react.{Callback, ReactNode, ReactElement, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js.Date

object TaskViews {

  val optDateL = CommonComponent.latchedRender[Option[Long]]("date") {
    case Some(l) => <.span(new Date(l).toLocaleString())
    case _ => <.span("No date")
  }

  val due = ReactComponentB[Due]("Due")
    .initialState_P(due => Latched.lazily(
      for {
        groups <- GroupService.myGroups.request
      } yield {
        val ids = groups.map(_.item.id.id).asIds[Group]
        due.due(ids)
      }
    ))
    .render_S( c => optDateL(c))
    .build


  val taskAdmin = ReactComponentB[WithPerms[Task]]("TaskInfo")
    .render_P { wp =>
      if (wp.perms("edit")) {
        <.div(
          <.a(^.href:= MainRouter.TaskOutputsP.path(wp.item.id), "View submissions")
        )
      } else <.div()
    }
    .build

  /**
   * Summary information for a task
   */
  val taskInfo = ReactComponentB[WithPerms[Task]]("TaskInfo")
    .render_P({ wp =>
      val task = wp.item
      val name = task.details.name.getOrElse("Untitled task")

      <.div(
        <.h3(
          <.a(^.href := MainRouter.TaskP.path(wp.item.id), name)
        ),
        taskAdmin(wp),
        <.p(task.details.description.getOrElse(""):String),
        if (wp.perms("edit")) {
          <.div(

          )
        } else {
          <.div(
            <.div(^.className := "text-info", "opens: ", due(task.details.open)),
            <.div(^.className := "text-danger", "closes: ", due(task.details.closed)),
            <.p()
          )
        }
      )
    })
    .build


  /**
   * Summary info for each task in a list
   */
  val taskInfoList = CommonComponent.latchedRender[Seq[WithPerms[Task]]]("TaskInfoList") { groups =>
    <.div(
      for { g <- groups } yield taskInfo(g)
    )
  }

  /**
   * Summary info for each task in a course
   */
  val courseTasks = ReactComponentB[Id[Course, String]]("CourseTasks")
    .initialState_P(s => TaskService.courseTasks(s))
    .render_S { c => taskInfoList(c) }
    .build

  /**
   * View for a particular task (allowing the user to do it)
   */
  val taskView = CommonComponent.latchedRender[WithPerms[Task]]("TaskView") { wp =>
    <.div(
      Front.siteHeader(""),

      <.div(^.className := "container",
        CourseViews.courseInfoL(CourseService.latch(wp.item.course)),
        taskInfo(wp),
        if (wp.perms("complete")) {
          editOutputForTask(wp.item)
        } else {
          viewOutputForTask(wp.item)
        }
      )
    )
  }

  val taskFront = ReactComponentB[Id[Task,String]]("TaskFront")
    .initialState_P(id => TaskService.latch(id))
    .render_S({ s => taskView(s) })
    .build

  val viewOutputForTask = ReactComponentB[Task]("viewOutputForTask")
    .render_P(task =>
      task.body match {
        case c:CritiqueTask => <.div("Sorry, this task doesn't appear to be open. (If you think it should be open, try refreshing the page -- maybe it's opened since I cached it.)")
        case v:VideoTask => VideoViews.front(task)
      }
    ).build


  val editOutputForTask = ReactComponentB[Task]("taskOutputForTask")
    .render_P(task =>
      task.body match {
        case c:CritiqueTask => CritiqueViews.frontTwo(task)
        case v:VideoTask => VideoViews.front(task)
        case q:QuestionnaireTask => QuestionnaireTaskViews.front(task)
        case _ => <.div("Oops, we haven't created the view for this task type yet")
      }
    )
    .build


  /**
    * Shows a preview of a task output
    * FIXME: Currently only matches videos
    */
  def preview(taskBody:TaskBody, toBody:TaskOutputBody, incContext:Boolean=false):ReactElement = (taskBody, toBody) match {
    case (ct:CritiqueTask, c:Critique) => {
      if (incContext) {
        <.div(
          <.div(^.cls := "panel panel-default",
            <.div(^.cls := "panel-heading", "This is a critique of:"),
            <.div(^.cls := "panel-body",
              CritiqueViews.renderCritTarget(c.target)
            )
          ),
          preview(ct.task, c.task)
        )
      } else {
        <.div(
          preview(ct.task, c.task)
        )
      }
    }
    case (vt:VideoTask, vto:VideoTaskOutput) => vto.video match {
      case Some(YouTube(ytId)) =>
        <.div(
          VideoViews.youTubePlayer(ytId)
        )
      case None => <.div(
        "This critique is not ready to view yet -- student has created a critique but not yet posted a video ID"
      )
      case x => <.div(
        "Unrenderable preview: ", x.toString
      )
    }
    case (qt:QuestionnaireTask, qto:QuestionnaireTaskOutput) =>
      val pairs = qto.answers.map({ a =>
        val q = qt.questionMap(a.question)
        (q, a)
      }).filter(!_._1.hideInCrit)

      QuestionViews.viewQuestionnaireAnswers(pairs)
    case _ => <.div(
      <.div("Unrenderable content")
    )
  }

  def preview(task:Task, taskOutput:TaskOutput, anonymous:Boolean):ReactElement = {
    if (anonymous) preview(task.body, taskOutput.body) else <.div(
      <.label("by: ", TargetViews.name(taskOutput.by)),
      preview(task.body, taskOutput.body)
    )
  }

  def preview(toId:Id[TaskOutput,String]):ReactNode = {
    CommonComponent.futureNode {
      for {
        wpTO <- TaskOutputService.future(toId)
        wpT <- TaskService.latch(wpTO.item.task).request
      } yield preview(wpT.item.body, wpTO.item.body)
    }
  }


  /*
   * All outputs, for marking
   */

  case class Selection[T,C](private var _selected: Option[T], seq:Seq[T], context:C) {
    def selected_=(o:Option[T]) = {
      this._selected = o
      WebApp.rerender()
    }

    def selected = _selected
  }

/*
  val allocationsSwitch = ReactComponentB[Selection[TaskOutput,Task]]("outputSwitch")
    .render_P { sel =>
      <.ul(^.className := "nav nav-pills", ^.role := "group",
        for ((output, idx) <- sel.seq.sortBy(_.by.toString).zipWithIndex) yield {
          <.li(^.className := (if (sel.selected == Some(output)) "active" else ""), ^.role := "presentation",
            <.a(^.onClick --> Callback { sel.selected = Some(output) },
              output.by match {
                case TargetGroup(g) => GroupViews.groupNameId(g)
                case TargetUser(u) => idx
              }
            )
          )
        }
      )
    }
    .build
    */

  def allocationsSwitch[A,B](sel:Selection[A,B])(f: (A, Int) => ReactNode = { (el:A, idx:Int) => <.span(idx).render }):ReactNode = {
    <.ul(^.className := "nav nav-pills nav-stacked", ^.role := "group",
      for ((targ, idx) <- sel.seq.zipWithIndex) yield {
        <.li(^.className := (if (sel.selected == Some(targ)) "active" else ""), ^.role := "presentation",
          <.a(^.onClick --> Callback { sel.selected = Some(targ) },
            f(targ, idx)
          )
        )
      }
    )
  }

  val allOutputs = ReactComponentB[Task]("all outputs")
    .initialState_P(task => Latched.lazily{
      for {
        outputs <- TaskOutputService.allOutputs(task.id)
      } yield new Selection(None, outputs, task)
    })
    .render_S({ state =>
      CommonComponent.latchR(state) { sel =>
        <.div(^.cls := "row",
          <.div(^.cls := "col-sm-2",
            allocationsSwitch(sel)({ case (output, idx) =>
              output.body match {
                case ct:Critique => <.span(
                  <.span(
                    TargetViews.name(output.by),
                    " critiques ",
                    TargetViews.name(ct.target)
                  )
                )
                case _ => <.span(TargetViews.name(output.by))
              }
            })
          ),
          <.div(^.cls := "col-sm-10",
            sel.selected match {
              case Some(target) => preview(sel.context.body, target.body, true)
              case _ => <.div()
            }
          )
        )

      }
    })
    .build

  val aoView = CommonComponent.latchedRender[WithPerms[Task]]("TaskView") { wp =>
    <.div(
      Front.siteHeader(""),

      <.div(^.className := "container",
        CourseViews.courseInfoL(CourseService.latch(wp.item.course)),
        taskInfo(wp),
        allOutputs(wp.item)
      )
    )
  }

  val allOutputsFront = ReactComponentB[Id[Task,String]]("TaskFront")
    .initialState_P(id => TaskService.latch(id))
    .render_S({ s => aoView(s) })
    .build


}

