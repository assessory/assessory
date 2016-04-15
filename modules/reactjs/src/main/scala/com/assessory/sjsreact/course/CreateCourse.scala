package com.assessory.sjsreact.course

import com.assessory.api.client.WithPerms
import com.assessory.sjsreact._
import com.assessory.sjsreact.services.CourseService
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.Course
import japgolly.scalajs.react.{Callback, BackendScope, ReactComponentB, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future

object CreateCourse {

  case class CreateCourseFormState(course:Course, result:Future[Option[WithPerms[Course]]] = Future.successful(None))

  class CreateCourseFormBackend($: BackendScope[Unit, CreateCourseFormState]) {


    def modCourse(c:Course):Callback = $.modState(state => state.copy(course = c))

    def save():Callback = $.modState { state =>
      val result = CourseService.createCourse(state.course).map(Some(_))
      result.onSuccess { case Some(wp) =>
        MainRouter.goTo(new MainRouter.CourseP(wp.item.id.id))
        WebApp.rerender()
      }
      state.copy(result=result)
    }

    def render(state:CreateCourseFormState) = {
      val course = state.course

      Front.contain(
        <.div(
          <.h2("Create Course"),
          <.form(
            <.div(^.cls := "form-group",
              <.label(^.`for` := "courseShortName", "Short name"),
              <.input(^.`type` := "text", ^.cls := "form-control", ^.value := course.shortName.getOrElse(""),
                ^.onChange ==> { (evt:ReactEventI) => modCourse(course.copy(shortName = Option(evt.target.value))) }
              )
            ),
            <.div(^.cls := "form-group",
              <.label(^.`for` := "courseTitle", "Title"),
              <.input(^.`type` := "text", ^.cls := "form-control", ^.value := course.title.getOrElse(""),
                ^.onChange ==> { (evt:ReactEventI) => modCourse(course.copy(title = Option(evt.target.value))) }
              )
            ),
            <.div(^.cls := "form-group",
              <.label(^.`for` := "courseTitle", "Title"),
              <.textarea(^.cls := "form-control", ^.value := course.shortDescription.getOrElse(""),
                ^.onChange ==> { (evt:ReactEventI) => modCourse(course.copy(shortDescription = Option(evt.target.value))) }
              )
            ),
            <.div(^.cls := "form-group",
              <.button(^.className := "btn btn-primary ", ^.onClick --> save(), "Save"),
              CommonComponent.futErrorResult(state.result)
            )
          )


        )
      )
    }

  }


  val form = ReactComponentB[Unit]("Create Course form")
    .initialState(CreateCourseFormState(new Course(id = invalidId, addedBy=invalidId)))
    .renderBackend[CreateCourseFormBackend]
    .buildU


}
