package org.assessory.vclient.group

import com.assessory.api.client.WithPerms
import com.wbillingsley.handy.{Id, Latch}
import com.assessory.api.appbase.{Course, CourseId, Group, GroupSet, GroupSetId}
import com.wbillingsley.veautiful.html.{<, DElement, VHtmlComponent, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.common.Front
import org.assessory.vclient.course.CourseViews
import org.assessory.vclient.services.{GroupService, GroupSetService}
import org.scalajs.dom.html

import scala.concurrent.Future

object GroupViews {

  def myGroups(c:CourseId):VHtmlNode = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val html = Latch.lazily(for
      groupSets <- GroupSetService.groupSetsInCourse(c)
      myGroups <- GroupService.myGroupsInCourse(c)
    yield
      <.div(
        for
          gs <- groupSets
          myGs = myGroups.map(_.item).filter(_.set == gs.id)
        yield
          if myGs.isEmpty then
            <.div(
              <.h3(<("small")(groupSetLink(gs))),
              "You are not in a group"
            )
          else
            <.div(
              for g <- myGs yield
                <.div(
                  <.h3(<("small")(groupSetLink(gs))),
                  g.name.getOrElse("Group " + g.id.id)
                )
            )
      )
    )

    LatchRender(html) { html => <.div(html) }
  }

  def groupSetLink(gs:GroupSet):DElement[html.Element] = {
    val name = gs.name.getOrElse("Untitled group set")
    <.a(^.href := Routing.GroupSetRoute(gs.id).path, <.span(name))
  }


  def groupSetView(gsId:GroupSetId) = LatchRender(GroupSetService.latch(gsId)) { wp =>
    val gs = wp.item

    <.div(
      Front.siteHeader,

      <.div(^.cls := "container",
        CourseViews.courseInfo(gs.course),
        GroupChooser(gs)
      )
    )
  }

  case class GroupChooser(gs:GroupSet) extends VHtmlComponent {

    import concurrent.ExecutionContext.Implicits.global

    val myGroup = Latch.lazily(GroupService.myGroupsInSet(gs).map(_.headOption))
    val allGroups = GroupService.allGroupsInSet(gs)

    def leave(group: Group):Unit = {
      for
        g <- GroupService.leaveGroup(group.id)
      do
        myGroup.clear()
        rerender()
    }

    def join(group: Group):Unit = {
      for
        g <- GroupService.joinGroup(group.id)
      do
        myGroup.clear()
        rerender()
    }

    def show:Latch[VHtmlNode] =
      for
        myG <- myGroup
        allG <- allGroups.request
      yield
        <.div(
          <.h3(gs.name.getOrElse("Group set " + gs.id.id)),
          myG match {
            case Some(g) =>
              <.div(
                "You are in ", <("b")(g.name.getOrElse(g.id.id)), " ",
                <.button(^.cls := "btn btn-secondary", "Leave", ^.onClick --> {
                  leave(g)
                })
              )
            case None =>
              <.div(
                <.table(^.cls := "table",
                  for g <- allG yield
                    <.tr(
                      <.td(g.name.getOrElse(s"Group ${g.id.id}")),
                      <.td(<.button(^.cls := "btn btn-secondary", "Join", ^.onClick --> {
                        join(g)
                      }))
                    )
                  )
              )
          }
        )

    def render = <.div(LatchRender(show)({ html => <.div(html) }))

  }

}
