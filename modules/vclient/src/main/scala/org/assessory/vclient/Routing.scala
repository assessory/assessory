package org.assessory.vclient

import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.Course
import com.wbillingsley.veautiful.PathDSL
import com.wbillingsley.veautiful.PathDSL./#
import com.wbillingsley.veautiful.html.{<, VHtmlNode}
import com.wbillingsley.veautiful.templates.HistoryRouter
import org.assessory.vclient.common.Front
import org.assessory.vclient.user.LoginViews

object Routing {

  sealed trait Route {
    def render: VHtmlNode
    def path: String
  }

  case object Home extends Route {
    def path = (/# / "").stringify
    def render = Front.front
  }

  case object Login extends Route {
    def path = (/# / "login").stringify
    def render = LoginViews.Login
  }

  case class CourseRoute(id:Id[Course, String]) extends Route {
    def path:String = (/# / "course" / id.id).stringify
    def render = <.div("course") // TODO: implement
  }


  object Router extends HistoryRouter[Route] {
    override var route: Route = Home

    override def render: VHtmlNode = route.render

    override def path(route: Route): String = route.path

    override def routeFromLocation(): Route = PathDSL.hashPathArray() match {
      case Array("login") => Login
      case _ => Home
    }

    def rerender():Unit = renderElements(route.render)
  }

}