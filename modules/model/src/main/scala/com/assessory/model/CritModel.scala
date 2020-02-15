package com.assessory.model

import java.io.StringWriter

import au.com.bytecode.opencsv.CSVWriter
import com.assessory.api._
import com.assessory.api.client.WithPerms
import com.assessory.api.critique._
import com.assessory.api.question._
import com.assessory.api.video.{VideoTask, VideoTaskOutput}
import com.assessory.api.wiring.Lookups._
import com.assessory.asyncmongo._
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{Group, User, UserError}

import scala.util.Random

object CritModel {


  /**
   * Allocates within these groups, taking no account of groups' parents
 *
   * @param num
   * @return
   */
  def allocateTheseGroups(inGroups:Seq[Group], inRegs:Seq[Group.Reg], num:Int) = {
    import scala.collection.mutable

    val groups = inGroups.sortBy(_.id.id)
    val regs = inRegs

    println("ALLOCATING " + groups.map(_.name) + s"for ${regs.size} students")

    val groupIds = groups.map(_.id)
    val memberMap = for {
      (g,r) <- regs.groupBy(_.target)
    } yield g -> r.map(_.user).toSet

    val reverseMap = (for (g <- groupIds) yield g -> mutable.Set.empty[Id[User,String]]).toMap
    val forwardMap = (for (r <- regs; u = r.user) yield u -> mutable.Set.empty[Id[Group,String]]).toMap

    def pick(u:Id[User,String]) = {
      val g = groupIds
      val filtered = groupIds.filterNot(g => memberMap(g).contains(u) || reverseMap(g).contains(u))
      try {
        filtered.minBy(reverseMap(_).size)
      } catch {
        case x:Throwable =>
          println("ERRORED FOR" + groups.map(_.name) + s"for ${regs.size} students")
          throw x
      }

    }

    val shuffled = scala.util.Random.shuffle(regs)

    for {
      i <- 1 to num
      reg <- shuffled
    } {
      val g = pick(reg.user)
      reverseMap(g).add(reg.user)
      forwardMap(reg.user).add(g)
    }

    forwardMap
  }

  def allocateGroups(inGroups:Seq[Group], num:Int, task:Id[Task,String]):RefMany[CritAllocation] = {
    for {
      (rParent, groups) <- inGroups.groupBy(_.parent).iterator.toRefMany
      groupIds = groups.map(_.id)
      registrations <- RegistrationDAO.group.byTargets(groupIds).collect

      critMap = allocateTheseGroups(groups, registrations, num)

      (u, gIds) <- critMap.iterator.toRefMany

      unsaved = CritAllocation(
        id = CritAllocationDAO.allocateId.asId,
        task = task,
        completeBy = TargetUser(u),
        allocation = for (g <- gIds.toSeq) yield AllocatedCrit(target=TargetGroup(g))
      )

      saved <- CritAllocationDAO.saveNew(unsaved)
    } yield saved
  }


  def allocateTask(a:Approval[User], rTask:RefWithId[Task]):RefMany[CritAllocation] = {
    for {
      t <- rTask
      approved <- a ask Permissions.EditCourse(t.course)

      (set, num) <- t.body match {
        case CritiqueTask(AllocateStrategy(TTGroups(set), num), task) => (set, num).itself
        case _ => RefFailed(UserError("I can only allocate group crit tasks"))
      }
      groups <- GroupDAO.bySet(set).collect
      alloc <- allocateGroups(groups.toSeq, num, t.id)
    } yield alloc
  }


  def allocations(rTask:RefWithId[Task]) = {
    for (t <- CritAllocationDAO.byTask(rTask)) yield t
  }

  private def allocationsFor(a:Approval[User], task:Task):RefMany[Target] = {
    task.body match {
      case CritiqueTask(strategy, containedTask) =>
        strategy match {
          case AllocateStrategy(what, num) =>
            for {
              u <- a.who
              alloc <- CritAllocationDAO.byUserAndTask(u.itself, task.itself)
              ac <- alloc.allocation.toRefMany
            } yield ac.target
          case TargetMyStrategy(inTask, what, num) =>
            for {
              group <- GroupModel.myGroupsInCourse(a, a.cache(task.course.lazily)) if Some(group.set) == task.details.groupSet
              to <- TaskOutputDAO.byTaskAndAttn(inTask.lazily, TargetGroup(group.id))
            } yield TargetTaskOutput(to.id)
        }
    }
  }

  def myAllocations(a:Approval[User], rTask:RefWithId[Task]):RefMany[Target] = {
    for {
      u <- a.who
      t <- rTask
      ct <- t.body match {
        case ct:CritiqueTask => ct.itself
        case _ => RefFailed(UserError("This was not a critique task"))
      }
      target <- allocationsFor(a, t)
    } yield target
  }


  def byFromTask(a:Approval[User], t:Task):Ref[Target] = {

    // Get the user's groups in the corresponding groupSet, if it's not an individual assignment
    val groups:RefMany[Group] = for {
      u <- a.who
      gsId <- t.details.groupSet.toRef if !t.details.individual
      gs <- a.cache(gsId.lazily)
      g <- GroupModel.myGroupsInCourse(a, t.course.lazily) if g.set == gs.id
    } yield g

    // Pick the first group
    val targetGroup:RefOpt[TargetGroup] = for {
      u <- a.who
      g <- groups.first
    } yield TargetGroup(g.id)

    // Return the group, or the user if there isn't one
    for {
      u <- a.who.require
      t <- (targetGroup orElse RefSome(TargetUser(u.id))).require
    } yield t
  }


  private def blankAnswer(q:Question) = q match {
    case s:ShortTextQuestion => ShortTextAnswer(s.id, None)
    case b:BooleanQuestion => BooleanAnswer(b.id, None)
    case v:VideoQuestion => VideoAnswer(v.id, None)
    case f:FileQuestion => FileAnswer(f.id, None)
  }

  /**
   * Creates a new TaskOutput for a critique, with blank answers
    *
    * @param by
   * @param task
   * @param target
   * @return
   */
  private def createCrit(by:Target, task:Task, target:Target):Ref[TaskOutput] = {

    def blankFor(t:TaskBody):TaskOutputBody = t match {
      case qt:QuestionnaireTask => QuestionnaireTaskOutput(answers = for {
        q <- qt.questionnaire
      } yield blankAnswer(q))
      case vt:VideoTask => VideoTaskOutput(video = None)
      case ct:CritiqueTask => Critique(target = target, task = blankFor(ct.task))
    }

    task.body match {
      case ct:CritiqueTask =>
        val unsaved = TaskOutput(
          id = TaskOutputDAO.allocateId.asId,
          by = by,
          attn = Seq(target),
          task = task.id,
          body = Critique(
            target = target,
            task = blankFor(ct.task)
          )
        )
        TaskOutputDAO.saveSafe(unsaved)
      case _ =>
        RefFailed(UserError("I can only create critiques for critique tasks"))
    }
  }

  /**
   * Looks up a user's TaskOutput for a critique, or creates a blank one if there isn't one
    *
    * @param a
   * @param rTask
   * @param target
   * @return
   */
  def findOrCreateCrit(a:Approval[User], rTask:Ref[Task], target:Target):Ref[WithPerms[TaskOutput]] = {
    for {
      t <- rTask
      completeBy <- byFromTask(a, t)
      taskOutputs <- TaskOutputDAO.byTaskAndBy(t.id, completeBy).withFilter(
        _.body match {
          case Critique(storedTarget, _) if storedTarget == target => true
          case _ => false
        }
      ).collect
      to <- RefOpt(taskOutputs.headOption) orElse createCrit(completeBy, t, target)
      wp <- TaskOutputModel.withPerms(a, to)
    } yield wp
  }

  def findCritForAlloc(a:Approval[User], rca:Ref[CritAllocation], target:Target) = {
    for {
      u <- a.who
      ca <- rca
      alloc <- ca.allocation.find(_.target == target).toRef orFail UserError("This allocation doesn't include that target")
      crit <- alloc.critique.lazily orElse (for {
        wp <- findOrCreateCrit(a, a.cache.lookUp(ca.task), target)
        updatedAlloc <- CritAllocationDAO.setOutput(ca.id, target, wp.item.id)
      } yield wp.item)
    } yield crit
  }


  /** Fetches allocations as a CSV. */
  def allocationsAsCSV(a:Approval[User], rTask:Ref[Task]):Ref[String] = {
    val sWriter = new StringWriter()
    val writer = new CSVWriter(sWriter)

    val lineArrays = for {
      t <- rTask
      c <- a.cache.lookUp(t.course)
      approved <- a ask Permissions.EditCourse(c.itself)
      allocC <- CritAllocationDAO.byTask(t.itself).collect
      alloc <- allocC.toRefMany
      by <- TaskOutputModel.targetAsCsvString(a, alloc.completeBy)
      allocLine <- alloc.allocation.toRefMany
      targ <- TaskOutputModel.targetAsCsvString(a, allocLine.target)
    } yield (by ++ targ).toArray

    for { lines <- lineArrays.collect } yield {
      for { line <- lines } writer.writeNext(line)
      writer.close()
      sWriter.toString
    }
  }

  /**
    * Filters a RefMany with a possibly asynchronous filter
    * TODO: Move this onto RefMany
    */
  def filtering[T](r:RefMany[T], p:(T => Ref[Boolean])):RefMany[T] = {
    for {
      item <- r
      test <- p(item) if test
    } yield item
  }

  def isBy(to:TaskOutput, t:Target):Ref[Boolean] = {
    t match {
      case TargetGroup(gId) => gId.lazily.flatMap(isBy(to, _))
      case TargetUser(uId) => uId.lazily.flatMap(isBy(to, _))
    }
  }

  /**
    * Returns true if this task output should be considered as "by the group" (or one of their users, etc)
    */
  def isBy(to:TaskOutput, g:Group):Ref[Boolean] = targetIncludes(to.by, g)

  /**
    * Returns true if this task output should be considered as "by the user" (or their group, etc)
    */
  def isBy(to:TaskOutput, u:User):Ref[Boolean] = targetIncludes(to.by, u)

  /**
    * Returns true if this Target relates to this Group
    */
  def targetIncludes(by:Target, g:Group):Ref[Boolean] = by match {
    case TargetUser(uId) => for {
      rs <- GroupModel.registrationsInGroup(g.itself).collect
    } yield rs.exists(_.user == uId)
    case TargetGroup(gId) => (gId == g.id).itself
  }

  /**
    * Returns true if this Target relates to this User
    */
  def targetIncludes(by:Target, u:User):Ref[Boolean] = by match {
    case TargetUser(uId) => (uId == u.id).itself
    case TargetGroup(gId) => for {
      rs <- GroupModel.registrationsInGroup(gId.lazily).collect
    } yield rs.exists(_.user == u.id)
    case TargetCourseReg(cId) => for {
      cr <- cId.lazily
    } yield cr.user == u.id
  }

  /**
    * Checks that Group g's parent group (if there is one) contains the target
    */
  def parentOk(t:Target, g:Group):Ref[Boolean] = g.parent match {
    case Some(pId) => for {
      gParent <- pId.lazily
      parentMatch <- t match {
        case TargetUser(uId) => (for {
          u <- uId.lazily
          gs <- gParent.set.lookUp 
          g <- GroupModel.myGroupInSet(u, gs) if g.id == gParent.id
        } yield true) orElse false.itself
        case TargetGroup(gId) => for {
          g <- gId.lazily
        } yield pId == gParent.id
      }
    } yield parentMatch
    case _ => true.itself
  }


  /**
    * Allocate me n things to critique, that I didn't write, choosing the ones that have been critiqued the fewest
    * times
    */
  def allocateMe(by:Target, task:Task, t:TargetType, num:Int, alreadyDone:Seq[Target]):Ref[Seq[Target]] = {
    t match {
      case TTOutputs(id) =>
        for {
          crits <- TaskOutputDAO.byTask(task.itself).collect
          outputs = TaskOutputDAO.byTask(id.lazily).withFilter({ case to => to.finalised.nonEmpty && !alreadyDone.contains(TargetTaskOutput(to.id)) })
          toCrit <- filtering[TaskOutput](outputs, { x => isBy(x, by).map(!_) }).collect
        } yield {
          val critCounts = crits.collect(
            { case TaskOutput(_, _, _, _, Critique(TargetTaskOutput(toId), _), _, _, _) => toId}
          ).groupBy(identity).mapValues(_.size)

          val selected = Random.shuffle(toCrit).sortBy({ c => critCounts.getOrElse(c.id, 0) }).take(num)
          selected.map({ to => TargetTaskOutput(to.id) })
        }
      case TTGroups(gsId) => for {
        gs <- gsId.lazily
        crits <- TaskOutputDAO.byTask(task.itself).collect

        groupsRemaining = GroupDAO.bySet(gsId).withFilter({ case t => !alreadyDone.contains(TargetGroup(t.id)) })

        // check they are from the same tutorial (if things are divided into tutorials)
        groups = filtering[Group](groupsRemaining, { g => parentOk(by, g) })

        // don't allocate a group to be critiqued by itself or one of its users
        toCrit <- filtering[Group](groups, { g => targetIncludes(by, g).map(!_) }).collect
      } yield {
        val critCounts = crits.collect(
          { case TaskOutput(_, _, _, _, Critique(TargetGroup(gId), _), _, _, _) => gId}
        ).groupBy(identity).mapValues(_.size)

        val selected = Random.shuffle(toCrit).sortBy({ c => critCounts.getOrElse(c.id, 0) }).take(num)
        selected.map({ to => TargetGroup(to.id) })
      }
    }
  }

  def fillUp(by:Target, task:Task, t:TargetType, num:Int):RefMany[TaskOutput] = {
    for {
      existing <- TaskOutputDAO.byTaskAndBy(task.id, by).collect

      existingTargets = existing.map(_.body).collect { case ct:Critique => ct.target }

      extraTargs <- {
        if (num - existing.size > 0) allocateMe(by, task, t, num - existing.size, existingTargets) else Seq.empty[Target].itself
      }

      extraTOs <- (
        for {
          t <- extraTargs.toRefMany
          saved <- createCrit(by, task, t)
        } yield saved
      ).collect

      all = existing ++ extraTOs

      to <- all.toRefMany
    } yield to
  }


  def blankFor(taskBody:TaskBody):TaskOutputBody = taskBody match {
    case v:VideoTask => VideoTaskOutput(None)
      // FIXME: other task bodies
  }

  def strategyOf(t:Task) = t.body match {
    case CritiqueTask(strategy, _) => strategy.itself
    case _ => RefFailed(new IllegalArgumentException("Asked the critique strategy of a non-critique task"))
  }

  /**
   * Given a critique task, what stuff of mine could have been critiqued?
   */
  def targetMySource(task:Task, u:User):RefMany[Target] = {
    for {
      strategy <- strategyOf(task)

      tt <- strategy match {
        case AllocateStrategy(tt, _) => tt.itself
        case AnyStrategy(tt, _) => tt.itself
      }

      myTargs <- tt match {
        case TTOutputs(ttTask) => for {
          myOutput <- TaskOutputModel.myOutputs(Approval(RefSome(u)), ttTask.lazily)
        } yield TargetTaskOutput(myOutput.id)
        case TTGroups(gsId) => for {
          gs <- gsId.lazily
          g <- GroupModel.myGroupInSet(u, gs)
          manied <- Seq(g).toRefMany
        } yield TargetGroup(manied.id)
      }
    } yield {
      myTargs
    }
  }

  def makeTos(approval:Approval[User], task:Task):RefMany[TaskOutput] = task match {
    case Task(_, _, _, CritiqueTask(AllocateStrategy(TTOutputs(id), num), critTask)) =>
      for {
        u <- approval.who
        to <- fillUp(TargetUser(u.id), task, TTOutputs(id), num)
      } yield to
    case Task(_, _, _, CritiqueTask(AllocateStrategy(TTGroups(id), num), critTask)) =>
      for {
        u <- approval.who
        to <- fillUp(TargetUser(u.id), task, TTGroups(id), num)
      } yield to
    case Task(_, _, _, CritiqueTask(TargetMyStrategy(critTaskId, _, _), critTask)) =>
      for {
        u <- approval.who
        critTask <- critTaskId.lazily

        myTarget <- targetMySource(critTask, u)
        critiqueMy <- TaskOutputDAO.byTaskAndAttn(critTaskId, myTarget).withFilter(_.finalised.nonEmpty)
        to <- findOrCreateCrit(approval, task.itself, TargetTaskOutput(critiqueMy.id))
      } yield {
        to.item
      }
  }
}
