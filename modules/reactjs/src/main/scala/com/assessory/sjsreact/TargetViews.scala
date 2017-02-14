package com.assessory.sjsreact

import com.assessory.api.{TargetUser, TargetGroup, Target}
import com.assessory.sjsreact.user.UserViews
import japgolly.scalajs.react.ReactElement

import japgolly.scalajs.react.vdom.prefix_<^._

object TargetViews {

  def name(t:Target):ReactElement = {
    t match {
      case TargetGroup(gId) => GroupViews.groupNameId(gId)
      case TargetUser(uId) => UserViews.nameById(uId)
      case _ => <.span("Can't show the name for this type of target")
    }
  }


}
