package org.assessory.vclient.task

import com.assessory.api.{Target, TargetCourseReg, TargetGroup, TargetTaskOutput, TargetUser}
import com.wbillingsley.handy.{Latch, Ref}
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode}
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.group.GroupViews
import org.assessory.vclient.services.{GroupService, TaskOutputService, UserService}
import Ref._
import org.assessory.vclient.user.UserViews

import scala.concurrent.ExecutionContext.Implicits.global

object TargetViews {

  def displayName(t:Target):Ref[String] = t match {
    case TargetGroup(gId) => gId.lazily(GroupService.lookup).map(_.name.getOrElse("Unnamed group"))
    case TargetUser(id) => id.lazily(UserService.lu).map(UserViews.name)
    case TargetUser(id) => id.lazily(UserService.lu).map(UserViews.name)
    case TargetTaskOutput(id) => id.lazily(TaskOutputService.lookup).flatMap(to => displayName(to.by))
    case _ => t.getClass.getName.itself
  }


  case class ByLabel(t:Target) extends VHtmlComponent {

    val text:Latch[String] = Latch.lazily(displayName(t).toFuture)

    def render = <.span({
      LatchRender(text)({ n => <.span(n) }, none = <.span())
    })
  }

}
