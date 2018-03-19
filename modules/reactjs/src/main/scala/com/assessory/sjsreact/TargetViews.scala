package com.assessory.sjsreact

import com.assessory.api.{TargetTaskOutput, TargetUser, TargetGroup, Target}
import com.assessory.sjsreact.services.TaskOutputService
import com.assessory.sjsreact.user.UserViews
import japgolly.scalajs.react.{ReactNode, ReactElement}

import japgolly.scalajs.react.vdom.prefix_<^._

object TargetViews {

  def name(t:Target):ReactNode = {
    t match {
      case TargetGroup(gId) => GroupViews.groupNameId(gId)
      case TargetUser(uId) => UserViews.nameById(uId)
      case TargetTaskOutput(toId) => CommonComponent.futureNode({
        for {
          to <- TaskOutputService.future(toId)
        } yield name(to.item.by)
      })
      case _ => <.span("Can't show the name for this type of target")
    }
  }


}
