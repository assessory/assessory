package com.assessory.model

import java.io.{StringWriter, StringReader}

import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups._
import com.assessory.asyncmongo._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase._

import scala.collection.JavaConverters._

object CourseModel {

  def withPerms(a:Approval[User], c:Course) = {
    for {
      edit <- a.askBoolean(Permissions.EditCourse(c.itself))
      view <- a.askBoolean(Permissions.ViewCourse(c.itself))
    } yield {
      WithPerms(
        Map(
          "edit" -> edit,
          "view" -> view
        ),
      c)
    }
  }

  /**
   * Creates a course
   */
  def create(a:Approval[User], clientCourse:Course):Ref[WithPerms[Course]] = {
    for {
      u <- a.who orIfNone Refused("You must be logged in to create courses")
      approved <- a ask Permissions.CreateCourse

      // The client cannot set IDs, so we need to generate an ID for the course and the user's registration to it
      cid = if (CourseDAO.validId(clientCourse.id.id)) clientCourse.id else CourseDAO.allocateId.asId[Course]
      rid = RegistrationDAO.course.allocateId.asId[Course.Reg]

      // Create the course and registration
      unsavedCourse = clientCourse.copy(id=cid, addedBy=rid)
      reg = new Course.Reg(id=rid, user=u.id, target=cid, provenance=EmptyKind, roles=Set(CourseRole.staff))

      savedCourse <- CourseDAO.saveNew(unsavedCourse)
      savedReg <- RegistrationDAO.course.saveSafe(reg)

      wp <- withPerms(a, savedCourse)
    } yield wp
  }

  /**
   * Retrieves a course
   */
  def byId(a:Approval[User], id:Id[Course,String]) = {
    for {
      c <- a.cache(id.lazily)
      wp <- withPerms(a, c) // Ask for edit first, as it will default view
    } yield wp
  }

  /**
   * Retrieves a course
   */
  def findMany(a:Approval[User], ids:Ids[Course,String]) = {
    for {
      course <- ids.lookUp
    } yield course
  }

  /**
   * Fetches the courses this user is registered with.
   */
  def myCourses(a:Approval[User]):RefMany[WithPerms[Course]] = {
    for {
      u <- a.who
      courseIds <- RegistrationDAO.course.byUser(u.id).map(_.target).toIds
      c <- courseIds.lookUp
      wp <- withPerms(a, c)
    } yield wp
  }

  def usersInCourse(a:Approval[User], courseId:Id[Course, String]):RefMany[User] = {
    def rUserIds = (for {
      c <- RegistrationDAO.course.byTarget(courseId)
    } yield c.user).collect

    for {
      approved <- a ask Permissions.EditCourse(courseId.lazily)
      userIds <- rUserIds
      user <- userIds.map(_.id).asIds[User].lookUp
    } yield user
  }

}
