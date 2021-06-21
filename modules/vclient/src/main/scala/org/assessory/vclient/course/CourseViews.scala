package org.assessory.vclient.course

import com.assessory.api.client.WithPerms
import com.wbillingsley.handy.Id
import com.assessory.api.appbase._
import com.wbillingsley.veautiful.html.{<, DElement, VHtmlNode, ^}
import org.assessory.vclient.Routing
import org.assessory.vclient.common.Components.LatchRender
import org.assessory.vclient.common.Front
import org.assessory.vclient.group.GroupViews
import org.assessory.vclient.services.CourseService
import org.assessory.vclient.task.TaskViews
import org.scalajs.dom.html

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

  def courseInfo(id:Id[Course, String]):VHtmlNode = LatchRender(CourseService.latch(id)) { wp =>
    courseInfo(wp)
  }

  /**
   * The "album cover" for the course, as it would appear on the front page
   * @param wp
   * @return
   */
  def courseInfo(wp:WithPerms[Course]):DElement[html.Element] = {
    val course = wp.item

    <.div(^.cls := "course-info",
      <.div(^.cls := "media",
        <.div(^.cls := "mr-4",
          <.span(^.cls := "cover-image", <.img(^.src := wp.item.coverImage.getOrElse("http://placehold.it/100x100")))
        ),
        <.div(^.cls := "media-body",
          <.h4(^.cls := "mt-0",  course.shortName),
          <.h2(^.cls := "mt-0", <.a(^.href := Routing.CourseRoute(course.id).path, course.title)),
          courseAdmin(wp),
          <.p(wp.item.shortDescription)
        )
      ),

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

  /**
   * The front page of a course
   */
  def courseFront(c:Id[Course, String]):VHtmlNode = LatchRender(CourseService.latch(c)) { wp =>
    val course = wp.item

    <.div(
      Front.siteHeader,
      <.div(^.cls := "course-view",
        <.div(^.cls := "container",

          courseInfo(wp),

          <.div(^.cls := "row",
            <.div(^.cls := "col-sm-8",
              <.h3("Tasks"),
              <.div(
                TaskViews.courseTasks(course.id)
              )
            ),

            <.div(^.cls := "col-sm-4",
              <.h3("Groups"),
              GroupViews.myGroups(course.id)
            )
          )

        )
      )
    )
  }



}
