package com.assessory.api

import com.assessory.api.wiring.{Lookups}
import com.wbillingsley.handy.{
  Perm, Approval, Approved, Refused, autoGetsId, LookUpCache, HasId, GetsId,
  Ref, RefFailed, RefOpt, RefOptFailed, Id, Ids, refOps, lazily
}
import com.assessory.api.appbase._
import critique._
import due._

case class IdSeq[T](seq:Seq[Id[T, String]]) extends Ids[T, String]:
  def ids = seq.map(_.id)

given GetsId[Course, CourseId] = autoGetsId[Course, CourseId]
given GetsId[GroupSet, GroupSetId] = autoGetsId[GroupSet, GroupSetId]
given GetsId[Group, GroupId] = autoGetsId[Group, GroupId]
given GetsId[Task, TaskId] = autoGetsId[Task, TaskId]
given GetsId[TaskOutput, TaskOutputId] = autoGetsId[TaskOutput, TaskOutputId]
given GetsId[User, UserId] = autoGetsId[User, UserId]
given GetsId[CritAllocation, CritAllocationId] = autoGetsId[CritAllocation, CritAllocationId]
given [W, T, R, RT]: GetsId[Preenrolment[W, T, R, RT], PreenrolmentId[W, T, R, RT]] = autoGetsId[Preenrolment[W, T, R, RT], PreenrolmentId[W, T, R, RT]]


object Permissions {

  import wiring.Lookups.{given, *}

  def requireLoggedIn(who:RefOpt[User]):Ref[User] = (who orElse RefOptFailed(Refused("You are not logged in"))).require

  /**
   * Create a course
   */
  val CreateCourse = Perm.unique[User] { case (prior) =>
    for (u <- requireLoggedIn(prior.who)) yield Approved("Anyone may create a course")
  }

  val ViewCourse = Perm.onId[User, Course, Id[Course, String]]({ case (prior, course) =>
    hasAnyRole(course, requireLoggedIn(prior.who), CourseRole.roles, prior.cache)
  })

  val EditCourse = Perm.onId[User, Course, Id[Course, String]]({ case (prior, rCourse) =>
    hasRole(rCourse, requireLoggedIn(prior.who), CourseRole.staff, prior.cache)
  })

  val ViewGroupSet = Perm.onId[User, GroupSet, Id[GroupSet, String]] { case (prior, rGroupSet) =>
    for {
      gs <- rGroupSet
      a <- prior ask ViewCourse(gs.course)
    } yield Approved("Course viewers can view group sets")
  }

  val EditGroupSet = Perm.onId[User, GroupSet, Id[GroupSet, String]] { case (prior, rGroupSet) =>
    for {
      gs <- rGroupSet
      a <- prior ask EditCourse(gs.course)
    } yield Approved("Course editors can edit group sets")
  }

  val ViewGroup = Perm.onId[User, Group, Id[Group, String]] { case (prior, rGroup) =>
    for {
      g <- rGroup
      a <- prior ask ViewCourse(g.course).require
    } yield Approved("Course viewers can view groups")
  }

  val CreateGroup = Perm.onId[User, GroupSet, Id[GroupSet, String]] { case (prior, rGroupSet) =>
    for {
      g <- rGroupSet
      a <- prior ask EditGroupSet(g.itself)
    } yield Approved("If you can edit the group set, you can edit the group")
  }

  // You may join a group if you are in a course and not already in a group in the same set
  val JoinGroup = Perm.onId[User, Group, Id[Group, String]] { case (prior, rGroup) =>
    for
      u <- prior.who orFail Refused("Only logged in users can join groups")
      g <- rGroup
      a <- prior ask ViewGroup(g.itself)

      userGroupIds <- Lookups.groupRegistrationProvider.byUser(u.id).withFilter(_.roles.nonEmpty).map(_.target).collect
      userGroups <- Lookups.luGroup.many(userGroupIds).withFilter(_.set == g.set).collect

      notInGroups <- if userGroups.isEmpty then true.itself else RefFailed(Refused(s"You are already in group ${userGroups.head.name} "))
    yield Approved("You may join this group")
  }

  // You may leave a group if you are in it
  val LeaveGroup = Perm.onId[User, Group, Id[Group, String]] { case (prior, rGroup) =>
    for
      u <- prior.who orFail Refused("Only logged in users can leave groups")
      g <- rGroup

      reg <- Lookups.groupRegistrationProvider.byUserAndTarget(u.id, g.id) orFail Refused("You're not in this group")
    yield Approved("You may leave the group")
  }

  val EditGroup = Perm.onId[User, Group, Id[Group, String]] { case (prior, rGroup) =>
    for {
      g <- rGroup
      a <- prior ask (EditCourse(g.course)).require
    } yield Approved("Course editors can edit groups")
  }

  val ViewTask = Perm.onId[User, Task, Id[Task, String]] { case (prior, task) =>
    for (
        t <- task;
        a <- prior ask ViewCourse(t.course)
    ) yield a
  }

  val EditTask = Perm.onId[User, Task, Id[Task, String]] { case (prior, task) =>
    for (
        t <- task;
        a <- prior ask EditCourse(t.course)
    ) yield a
  }

  /** Whether this item was created by this user or their group */
  def isOwn(prior:Approval[User], who:User, by:By):Ref[Approved] = {
    by match {
      case By.ByUser(userId) => 
        for { 
          result <- if (UserId == who.id) Approved("Own work").itself else RefFailed(Refused("You may only edit your own work"))
        } yield result
      case By.ByGroup(gid) => (
        for {
          r <- Lookups.groupRegistrationProvider.byUserAndTarget(who.id, gid) orElse RefOptFailed(Refused("You may only edit your own work"))
         } yield Approved("Registered in group")
      ).require
    }
  }

  val EditOutput = Perm.onId[User, TaskOutput, Id[TaskOutput, String]] { case (prior, to) =>
    for {
      who <- requireLoggedIn(prior.who)
      o <- to
      task <- prior.cache(o.task)
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
      u <- requireLoggedIn(a.who)
      uId = u.id
      groupIds <- Lookups.groupRegistrationProvider.byUser(uId).map(_.target).collect
    } yield {
      // TODO: should we have a margin?
      val margin = 300000L

      def after(d:Due) = d.due(groupIds) match {
        case Some(l) => now - l
        case _ => 0L
      }

      after(t.details.open) > 0 && after(t.details.closed) < margin
    }
  }

  val WriteCritique = Perm.onId[User, CritAllocation, Id[CritAllocation, String]] { case (prior, gca) =>
      for {
        who <- requireLoggedIn(prior.who)
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
