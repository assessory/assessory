package org.assessory.vclient.user

import com.assessory.api.client.EmailAndPassword
import com.wbillingsley.handy.Latch
import com.wbillingsley.handy.appbase.UserError
import com.wbillingsley.veautiful.DiffNode
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Front
import org.scalajs.dom.{Element, Node}
import org.assessory.vclient.common.Components._
import org.assessory.vclient.services.UserService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object LoginViews {

  object Login extends VHtmlComponent {

    var email:String = ""
    var password:String = ""
    var error:Latch[String] = Latch.immediate("")

    def doLogin():Unit = {
      val ep = EmailAndPassword(email, password)
      val f = UserService.logIn(ep).map(_ => "Logged in")
      error = Latch.lazily(f)
      Routing.Router.routeTo(Routing.Home)
    }

    def socialLogin:VHtmlNode = {
      <.div(^.cls := "col-sm-6",
        <.h1("or"),
        <("form")(^.attr("action") := "/oauth/github", ^.attr("method") := "POST",
          <.button(^.cls := "btn btn-default", ^.attr("type") := "submit",
            <("i")(^.cls := "icon-github"), "Sign in with ", <("b")("GitHub")
          )
        )
      )
    }

    override protected def render: DiffNode[Element, Node] = {
      <.div(
        Front.siteHeader,

        <.div(^.cls := "container",
          <.div(^.cls := "row",

            <.div(^.cls := "col-sm-6",
              <.h1("Log In"),
              <("form")(^.cls := "form",
                <.div(^.cls := "form-group",
                  <.input(^.attr("type") := "text", ^.attr("placeholder") := "Email address", ^.prop("value") := email,
                    ^.on("input") ==> { e => e.inputValue.foreach(email = _); rerender() }
                  )
                ),
                <.div(^.cls := "form-group",
                  <.input(^.attr("type") := "password", ^.attr("placeholder") := "Password", ^.prop("value") := password,
                    ^.on("input") ==> { e => e.inputValue.foreach(password = _); rerender() }
                  )
                ),
                <.div(^.cls := "form-group",
                  <.button(^.cls := "btn btn-primary", ^.attr("disabled") ?= (if (password.isEmpty) Some("disabled") else None), ^.onClick --> doLogin(), "Log In"),
                  LatchRender(error)(e => <.div(e))
                )
              )
            ),

            socialLogin
          )
        )
      )
    }

  }

}
