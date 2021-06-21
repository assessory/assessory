package org.assessory.vclient.task

import com.assessory.api.{Target, TargetCourseReg, TargetGroup, TargetTaskOutput, TargetUser}
import com.wbillingsley.handy.{Latch, Ref, lazily, refOps}
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode}
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.group.GroupViews
import org.assessory.vclient.services.{GroupService, TaskOutputService, UserService}
import org.assessory.vclient.user.UserViews

import GroupService.given
import TaskOutputService.given
import UserService.given

import scala.concurrent.ExecutionContext.Implicits.global

object TargetViews {

  def displayName(t:Target):Ref[String] = t match {
    case TargetGroup(gId) => gId.lazily.map(_.name.getOrElse("Unnamed group"))
    case TargetUser(id) => id.lazily.map(UserViews.name)
    case TargetTaskOutput(id) => id.lazily.flatMap(to => displayName(to.by))
    case _ => t.getClass.getName.itself
  }


  case class ByLabel(t:Target) extends VHtmlComponent {

    val text:Latch[String] = Latch.lazily(displayName(t).toFuture)

    def render = <.span({
      LatchRender(text)({ n => <.span(n) }, none = <.span())
    })
  }

}
