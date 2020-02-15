package org.assessory.vclient.course

import com.assessory.api.client.WithPerms
import com.wbillingsley.handy.appbase.Course
import com.wbillingsley.veautiful.html.{<, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.services.CourseService

object CourseViews {

  /**
   * The "My Courses" block on the front page of Assessory
   * @return
   */
  def myCourses:VHtmlNode = LatchRender(CourseService.myCourses) {
    case courses if courses.nonEmpty =>
      <.div(^.cls := "container",
        <.h2("My Courses"),
        <.div(for { course <- courses } yield courseInfo(course))
      )
    case _ =>
      <.div(^.cls := "container")
  }

  /**
   * The "album cover" for the course, as it would appear on the front page
   * @param wp
   * @return
   */
  def courseInfo(wp:WithPerms[Course]):VHtmlNode = {
    val course = wp.item

    <.div(^.cls := "course-info",
      <.div(^.cls := "media",
        <.div(^.cls := "pull-left",
          <.span(^.cls := "cover-image", <.img(^.src := wp.item.coverImage.getOrElse("http://placehold.it/100x100")))
        )
      ),
      <.div(^.cls := "media-body",
        <.h4(^.cls := "media-heading",  course.shortName),
        <.h2(^.cls := "media-heading", <.a(^.href := Routing.CourseRoute(course.id).path, course.title)),
        courseAdmin(wp),
        <.p(wp.item.shortDescription)
      )

    )
  }

  /**
   * Provides admin links if the user has the necessary permissions
   */
  def courseAdmin(wp:WithPerms[Course]):VHtmlNode = {
    if (wp.perms("edit")) {
      <.div(<.a(^.href:=s"api/course/${wp.item.id.id}/autolinks.csv", "autolinks.csv"))
    } else <.div()
  }

}
