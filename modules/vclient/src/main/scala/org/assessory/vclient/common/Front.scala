package org.assessory.vclient.common

import com.wbillingsley.handy.Latch
import com.assessory.api.appbase.User
import com.wbillingsley.veautiful.html.{<, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.course.CourseViews
import org.assessory.vclient.services.UserService
import org.assessory.vclient.user.UserViews

object Front {

  def siteHeader: VHtmlNode = {
    <.div(^.cls := "site-header",
      <.div(^.cls := "navbar navbar-light navbar-expand justify-content-between",
        <.a(^.cls := "navbar-brand", "Assessory", ^.href := Routing.Home.path),
        <.div(loginStatus(UserService.self))
      )
    )
  }


  def loginStatus(l:Latch[Option[User]]):VHtmlNode = {
    LatchRender(l)({
      case Some(u) =>
        <.div(^.cls := "navbar-nav mr-auto test",
          <.a(^.cls := "nav-link nav-item", UserViews.name(u)),
          <.button(^.cls := "btn btn-outline-secondary nav-link nav-item", ^.onClick --> UserService.logOut(), "Log out"),
        )

      case None =>
        <.div(^.cls := "navbar-nav mr-auto test",
          <.a(^.cls := "nav-link nav-item", ^.href := Routing.Login.path, "Log in"),
          <.a(^.cls := "nav-link nav-item", ^.href := "", "Sign up")
        )
      }
    )
  }

  def front:VHtmlNode = {
    <.div(
      siteHeader,
      CourseViews.myCourses

    )
  }




}
