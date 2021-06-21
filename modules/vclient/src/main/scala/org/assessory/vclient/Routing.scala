package org.assessory.vclient

import com.wbillingsley.handy.Id
import com.assessory.api.appbase._
import com.wbillingsley.veautiful.PathDSL
import com.wbillingsley.veautiful.PathDSL.Compose./#
import com.wbillingsley.veautiful.html.{<, VHtmlNode}
import com.wbillingsley.veautiful.templates.HistoryRouter
import org.assessory.vclient.common.Front
import org.assessory.vclient.course.CourseViews
import org.assessory.vclient.user.LoginViews
import Id._
import com.assessory.api._
import com.wbillingsley.veautiful.logging.Logger
import org.assessory.vclient.task.TaskViews

object Routing {

  private val logger = Logger.getLogger(this.getClass)

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
    def render = TaskViews.taskFront(id)
  }

  case class TaskOutputRoute(id:Id[Task, String]) extends Route {
    def path:String = (/# / "task" / id.id / "outputs").stringify
    def render = TaskViews.allOutputs(id)
  }

  case class GroupRoute(id:Id[Group, String]) extends Route {
    def path:String = (/# / "group" / id.id).stringify
    def render = <.div("todo")
  }

  object Router extends HistoryRouter[Route] {
    var route: Route = Home

    override def render: VHtmlNode = {
      logger.info(s"Rendering route $route")
      route.render
    }

    override def path(route: Route): String = route.path

    override def routeFromLocation(): Route = {
      logger.info("Getting route from location")
      PathDSL.hashPathArray() match {
        case Array("login") => Login
        case Array("course", id) => CourseRoute(CourseId(id))
        case Array("task", id, "outputs") => TaskOutputRoute(TaskId(id))
        case Array("task", id) => TaskRoute(TaskId(id))
        case _ => Home
      }
    }

    def rerender():Unit = {
      logger.trace("Rerender")
      renderElements(route.render)
    }
  }

}
