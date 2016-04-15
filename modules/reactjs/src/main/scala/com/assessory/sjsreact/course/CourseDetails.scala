package com.assessory.sjsreact.course

import com.assessory.api.client.WithPerms
import com.assessory.sjsreact.{CourseViews, Front}
import com.wbillingsley.handy.appbase.Course
import japgolly.scalajs.react.vdom.prefix_<^._

object CourseDetails {

  def courseDetails(wp:WithPerms[Course]) = {
    val course = wp.item

    Front.contain(
      <.div(
        CourseViews.courseInfo(wp)
      )
    )
  }


}
