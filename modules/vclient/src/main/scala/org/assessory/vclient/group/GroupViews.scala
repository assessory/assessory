package org.assessory.vclient.group

import com.assessory.api.client.WithPerms
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{Course, Group, GroupSet}
import com.wbillingsley.veautiful.html.{<, DElement, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.services.{GroupService, GroupSetService}
import org.scalajs.dom.html

object GroupViews {

  def myGroups(c:Id[Course, String]):VHtmlNode = LatchRender(GroupService.myGroupsInCourse(c)) { groups =>
    <.div(
      for { g <- groups } yield groupInfo(g)
    )
  }

  def groupInfo(wp:WithPerms[Group]):VHtmlNode = {
    val group = wp.item
    val name = group.name.getOrElse("Untitled group")
    <.h3(
      <("small")(groupSetName(group.set)), <("br")(),
      <.a(^.href := Routing.GroupRoute(group.id).path, name)
    )
  }

  def groupSetName(gsId:Id[GroupSet, String]):VHtmlNode = {
    LatchRender(GroupSetService.latch(gsId)) { gs => groupSetName(gs)}
  }

  def groupSetName(wp:WithPerms[GroupSet]):DElement[html.Element] = {
    val name = wp.item.name.getOrElse("Untitled group set")
    <.span(name)
  }


}
