package com.assessory.api

import com.assessory.api.wiring.Lookups
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase._
import Ref._
import Ids._
import critique._
import due._

import wiring.Lookups._

object Permissions {

  def requireLoggedIn(who:RefOpt[User]):Ref[User] = (who orElse RefOptFailed(Refused("You are not logged in"))).require

  /**
   * Create a course
   */
  val CreateCourse = Perm.unique[User] { case (prior) =>
    for (u <- requireLoggedIn(prior.who)) yield Approved("Anyone may create a course")
  }

  val ViewCourse = Perm.onId[User, Course, String] { case (prior, course) =>
    hasAnyRole(course, requireLoggedIn(prior.who), CourseRole.roles, prior.cache)
  }

  val EditCourse = Perm.onId[User, Course, String] { case (prior, rCourse) =>
    hasRole(rCourse, requireLoggedIn(prior.who), CourseRole.staff, prior.cache)
  }

  val ViewGroupSet = Perm.onId[User, GroupSet, String] { case (prior, rGroupSet) =>
    for {
      gs <- rGroupSet
      a <- prior ask ViewCourse(gs.course)
    } yield Approved("Course viewers can view group sets")
  }

  val EditGroupSet = Perm.onId[User, GroupSet, String] { case (prior, rGroupSet) =>
    for {
      gs <- rGroupSet
      a <- prior ask EditCourse(gs.course)
    } yield Approved("Course editors can edit group sets")
  }

  val ViewGroup = Perm.onId[User, Group, String] { case (prior, rGroup) =>
    for {
      g <- rGroup
      a <- prior ask ViewCourse(prior.cache(g.course.lookUp orFail new IllegalStateException(s"Group ${g.id} had no course")))
    } yield Approved("Course viewers can view groups")
  }

  val EditGroup = Perm.onId[User, Group, String] { case (prior, rGroup) =>
    for {
      g <- rGroup
      a <- prior ask EditCourse(prior.cache(g.course.lookUp orFail new IllegalStateException(s"Group ${g.id} had no course")))
    } yield Approved("Course editors can edit groups")
  }

  val ViewTask = Perm.onId[User, Task, String] { case (prior, task) =>
    for (
        t <- task;
        a <- prior ask ViewCourse(t.course)
    ) yield a
  }

  val EditTask = Perm.onId[User, Task, String] { case (prior, task) =>
    for (
        t <- task;
        a <- prior ask EditCourse(t.course)
    ) yield a
  }

  def isOwn(prior:Approval[User], who:User, t:Target):Ref[Approved] = {
    t match {
      case TargetUser(uid) => {
        if (uid != who.id) {
          RefFailed(Refused("You may only edit your own work"))
        } else {
          Approved("Own work").itself
        }
      }
      case TargetCourseReg(cregId) => 
        for { 
          creg <- prior.cache(cregId.lazily) 
          result <- if (creg.user == who.id) Approved("Own work").itself else RefFailed(Refused("You may only edit your own work"))
        } yield result
      case TargetGroup(gid) => (
        for {
          r <- Lookups.groupRegistrationProvider.byUserAndTarget(who.id, gid) orElse RefOptFailed(Refused("You may only edit your own work"))
         } yield Approved("Registered in group")
      ).require
    }
  }

  val EditOutput = Perm.onId[User, TaskOutput, String] { case (prior, to) =>
    for {
      who <- prior.cache(requireLoggedIn(prior.who))
      o <- to
      task <- prior.cache(o.task.lazily)
      ownWork <- isOwn(prior, who, o.by) 
      open <- isOpen(prior, task)
      result <- if (open) Approved("You may edit this").itself else RefFailed(UserError("This task is closed"))
    } yield result
  }

  /**
   * Checks if a task is open for a user. 
   */
  def isOpen(a:Approval[User], t:Task):Ref[Boolean] = {
    val now = System.currentTimeMillis()

    for {
      uId <- requireLoggedIn(a.who).refId.require
      groupIds <- Lookups.groupRegistrationProvider.byUser(uId).map(_.target).collect
    } yield {
      // TODO: should we have a margin?
      val margin = 300000L

      def after(d:Due) = d.due(groupIds.map(_.id).asIds) match {
        case Some(l) => now - l
        case _ => 0L
      }

      after(t.details.open) > 0 && after(t.details.closed) < margin
    }
  }

  val WriteCritique = Perm.onId[User, CritAllocation, String] { case (prior, gca) =>
      for {
        who <- prior.cache(requireLoggedIn(prior.who))
        g <- gca
        ownWork <- isOwn(prior, who, g.completeBy)
      } yield Approved("You may write critiques that are allocated to you")
  }

  def getRoles(course: Ref[Course], user: Ref[User]) = {
    for {
       uId <- user.refId
       cId <- course.refId
       r <- Lookups.courseRegistrationProvider.byUserAndTarget(uId, cId)
    } yield r.roles
  }

  def hasRole(course:Ref[Course], user:Ref[User], role:CourseRole, cache:LookUpCache):Ref[Approved] = {
    (
      for (
        roles <- getRoles(course, user) if roles.contains(role)
      ) yield Approved(s"You have role $role for this course")
    ).orElse(RefOptFailed(Refused(s"You do not have role $role for this course"))).require
  }

  def hasAnyRole(course:Ref[Course], user:Ref[User], roles:Set[CourseRole], cache:LookUpCache):Ref[Approved] = {
    (
      for (
        savedRoles <- getRoles(course, user) if roles.intersect(savedRoles).nonEmpty
      ) yield Approved(s"You have any of these roles $roles for this course")
    ).orElse(RefOptFailed(Refused(s"You do not have any of these roles $roles for this course"))).require
  }
}
