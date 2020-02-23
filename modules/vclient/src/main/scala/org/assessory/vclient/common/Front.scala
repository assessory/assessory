package org.assessory.vclient.common

import com.wbillingsley.handy.Latch
import com.wbillingsley.handy.appbase.User
import com.wbillingsley.veautiful.html.{<, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.course.CourseViews
import org.assessory.vclient.services.UserService

object Front {

  def siteHeader: VHtmlNode = {
    <.div(^.cls := "site-header",
      <.div(^.cls := "navbar navbar-expand-sm navbar-static-top",
        <.div(^.cls := "container",
          <.a(^.cls := "navbar-brand", "Assessory", ^.href := Routing.Home.path),
          <.ul(^.cls := "nav navbar-nav"),
            loginStatus(UserService.self)
        )
      )
    )
  }


  def loginStatus(l:Latch[Option[User]]):VHtmlNode = {
    LatchRender(l) {
      case Some(u) =>
        val name: String = u.name.getOrElse("Anonymous")

        <.ul(^.cls := "nav navbar-nav pull-right",
          <.li(^.cls := "nav-item", <.button(^.cls := "btn btn-link nav-link", ^.onClick --> UserService.logOut(), "Log out")),
          <.li(^.cls := "nav-item", <.a(^.cls := "nav-link", name))
        )

      case None =>
        <.ul(^.cls := "nav navbar-nav pull-right",
          <.li(^.cls := "nav-item", <.a(^.cls := "nav-link", ^.href := Routing.Login.path, "Log in")),
          <.li(^.cls := "nav-item", <.a(^.cls := "nav-link", ^.href := "", "Sign up"))
        )
    }
  }

  def front:VHtmlNode = {
    <.div(
      siteHeader,
      CourseViews.myCourses

    )
  }




}
