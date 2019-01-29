package com.assessory.sjsreact

import com.assessory.api.client.EmailAndPassword
import com.assessory.sjsreact.services.{CourseService, UserService}
import com.wbillingsley.handy.Latch
import com.wbillingsley.handy.appbase.UserError
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future

object LogInViews {

  class LogInBackend($: BackendScope[_, (EmailAndPassword, Latch[String])]) {

    def email(e: ReactEventI) = $.modState { case (ep, ls) => (ep.copy(email = e.target.value), ls) }

    def password(e: ReactEventI) = $.modState { case (ep, ls) => (ep.copy(password = e.target.value), ls) }

    def logIn(e: ReactEventI) = $.modState { case (ep, ls) =>
      val v = UserService.logIn(ep).map(_ => "Logged in").recoverWith { case x => Future.failed(UserError("Log in failed")) }
      (ep, Latch.lazily(v))
    }

    def render(s:(EmailAndPassword, Latch[String])) =
      <.div(
        Front.siteHeader("Hello"),

        <.div(^.className := "container",
          <.div(^.className := "row",

            <.div(^.className := "col-sm-6",
              <.h1("Log In"),
              <.form(^.className := "form",
                <.div(^.className := "form-group",
                  <.input(^.`type`:="text", ^.placeholder:="Email address", ^.onChange ==> email)
                ),
                <.div(^.className := "form-group",
                  <.input(^.`type`:="password", ^.placeholder:="Password", ^.onChange ==> password)
                ),
                <.div(^.className := "form-group",
                  <.button(^.className:="btn btn-primary", ^.disabled := !s._2.isCompleted, ^.onClick ==> logIn, "Log In"),
                  CommonComponent.latchedString(s._2)
                )
              )
            ),

            socialLogIn()
          )
        )
      )
  }

  val socialLogIn = ReactComponentB[Unit]("Social Login")
    .render(_ =>
      <.div(^.className := "col-sm-6",
        <.h1("or"),
        <.form(^.action := "/oauth/github", ^.method := "POST",
          <.button(^.className:= "btn btn-default", ^.`type` := "submit",
            <.i(^.className := "icon-github"), "Sign in with ", <.b("GitHub")
          )
        )
      )
    )
    .buildU

  val logIn = ReactComponentB[Unit]("Front")
    .initialState { (EmailAndPassword("", ""), Latch.immediate("")) }
    .renderBackend[LogInBackend]
    .buildU


  class SignUpBackend($: BackendScope[_, (EmailAndPassword, Latch[String])]) {

    def email(e: ReactEventI) = $.modState { case (ep, ls) => (ep.copy(email = e.target.value), ls) }

    def password(e: ReactEventI) = $.modState { case (ep, ls) => (ep.copy(password = e.target.value), ls) }

    def logIn(e: ReactEventI) = $.modState { case (ep, ls) =>
      val v = UserService.signUp(ep).map(_ => "Logged in").recoverWith { case x => Future.failed(UserError("Sign up failed")) }
      (ep, Latch.lazily(v))
    }

    def render(state:(EmailAndPassword, Latch[String])) =
      <.div(
        Front.siteHeader("Hello"),

        <.div(^.className := "container",
          <.div(^.className := "row",

            <.div(^.className := "col-sm-6",
              <.h1("Sign Up"),
              <.form(^.className := "form",
                <.div(^.className := "form-group",
                  <.input(^.`type`:="text", ^.placeholder:="Email address", ^.onChange==> email)
                ),
                <.div(^.className := "form-group",
                  <.input(^.`type`:="password", ^.placeholder:="Password", ^.onChange==> password)
                ),
                <.div(^.className := "form-group",
                  <.button(^.className:="btn btn-primary", ^.disabled := !state._2.isCompleted, ^.onClick==> logIn, "Log In"),
                  CommonComponent.latchedString(state._2)
                )
              )
            ),

            socialLogIn()
          )
        )

      )
  }


  val signUp = ReactComponentB[Unit]("SignUp")
    .initialState((EmailAndPassword("", ""), Latch.immediate("")))
    .renderBackend[SignUpBackend]
    .buildU

}
