package com.assessory.model

import java.io.{StringWriter, StringReader}

import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import com.assessory.api.{given, _}
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups.{given, _}
import com.assessory.asyncmongo._
import com.wbillingsley.handy.{Ref, refOps, Approval, RefOpt, RefMany, HasKind, EmptyKind, Id, lazily}
import com.assessory.api.appbase._

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
      approved <- a ask Permissions.CreateCourse.itself
      u <- a.who.require

      // The client cannot set IDs, so we need to generate an ID for the course and the user's registration to it
      cid = if (CourseDAO.validId(clientCourse.id.id)) clientCourse.id else CourseId(CourseDAO.allocateId)
      rid = RegistrationId[Course, CourseRole, HasKind](RegistrationDAO.course.allocateId)

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
      c <- a.cache(id)
      wp <- withPerms(a, c) // Ask for edit first, as it will default view
    } yield wp
  }

  def byShortName(sn:String) = CourseDAO.byShortName(sn)

  /**
   * Retrieves a course
   */
  def findMany(a:Approval[User], ids:Seq[Id[Course,String]]) = ids.lookUp

  /**
   * Fetches the courses this user is registered with.
   */
  def myCourses(a:Approval[User]):RefMany[WithPerms[Course]] = {
    for {
      u <- a.who
      courseIds <- RegistrationDAO.course.byUser(u.id).map(_.target).collect
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
      user <- userIds.lookUp
    } yield user
  }

  def myRegistrationInCourse(u:Ref[User], c:Ref[Course]):RefOpt[Course.Reg] = {
    for {
      uId <- u.refId
      cId <- c.refId
      r <- RegistrationDAO.course.byUserAndTarget(uId, cId)
    } yield r
  }

  /**
    * Enrols a user into a course with a particular role. Note this has no security check and is not an external API.
    *
    */
  def forceEnrol(u:Ref[User], c:Ref[Course], r:Set[CourseRole]):Ref[Course.Reg] = {
    for {
      uId <- u.refId.require
      cId <- c.refId.require
      reg <- RegistrationDAO.course.register(uId, cId, r, EmptyKind)
    } yield reg
  }

}
