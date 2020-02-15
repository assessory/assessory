package org.assessory.vclient

import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{Course, Group}
import com.wbillingsley.veautiful.PathDSL
import com.wbillingsley.veautiful.PathDSL./#
import com.wbillingsley.veautiful.html.{<, VHtmlNode}
import com.wbillingsley.veautiful.templates.HistoryRouter
import org.assessory.vclient.common.Front
import org.assessory.vclient.course.CourseViews
import org.assessory.vclient.user.LoginViews
import Id._
import com.assessory.api.{Task, TaskOutput}

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
    def render = CourseViews.courseFront(id)
  }

  case class TaskRoute(id:Id[Task, String]) extends Route {
    def path:String = (/# / "task" / id.id).stringify
    def render = <.div("todo")
  }

  case class TaskOutputRoute(id:Id[Task, String]) extends Route {
    def path:String = (/# / "task" / id.id / "outputs").stringify
    def render = <.div("todo")
  }

  case class GroupRoute(id:Id[Group, String]) extends Route {
    def path:String = (/# / "group" / id.id).stringify
    def render = <.div("todo")
  }

  object Router extends HistoryRouter[Route] {
    override var route: Route = Home

    override def render: VHtmlNode = route.render

    override def path(route: Route): String = route.path

    override def routeFromLocation(): Route = PathDSL.hashPathArray() match {
      case Array("login") => Login
      case Array("course", id) => CourseRoute(id.asId[Course])
      case Array("task", id) => TaskRoute(id.asId[Task])
      case Array("task", id, "outputs") => TaskOutputRoute(id.asId[Task])
      case _ => Home
    }

    def rerender():Unit = renderElements(route.render)
  }

}
