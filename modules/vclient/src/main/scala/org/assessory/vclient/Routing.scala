package org.assessory.vclient

import com.wbillingsley.veautiful.PathDSL
import com.wbillingsley.veautiful.PathDSL./#
import com.wbillingsley.veautiful.html.{<, VHtmlNode}
import com.wbillingsley.veautiful.templates.HistoryRouter
import org.assessory.vclient.common.Front

object Routing {

  sealed trait Route {
    def render: VHtmlNode
    def path: String
  }

  case object Home extends Route {
    def path = (/# / "").stringify
    def render = Front.front
  }


  object Router extends HistoryRouter[Route] {
    override var route: Route = _

    override def render: VHtmlNode = route.render

    override def path(route: Route): String = route.path

    override def routeFromLocation(): Route = PathDSL.hashPathArray() match {
      case _ => Home
    }

    def rerender():Unit = renderElements(route.render)
  }

}
