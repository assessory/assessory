package com.assessory.model

import com.assessory.api.call._
import com.assessory.asyncmongo.UserDAO
import com.wbillingsley.handy.{Approval, Ref, RefFailed}
import com.wbillingsley.handy.appbase.{ActiveSession, User}
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy.Ids._
import com.assessory.api.wiring.Lookups._

object CallsModel {


  def call(a:Approval[User], c:Call):Ref[Return] = c match {

    case WithSession(a, c) => call(Approval(UserDAO.bySessionKey(a.key)), c)

    case Register(email, password, session) => UserModel.signUp(Some(email), Some(password), session).map(ReturnUser.apply)
    case Login(email, password, session) => UserModel.logIn(Some(email), Some(password), session).map(ReturnUser.apply).require

    case CreateCourse(c) => CourseModel.create(a, c).map { wp => ReturnCourse(wp.item) }
    case CreateTask(t) => TaskModel.create(a, t).map { wp => ReturnTask(wp.item) }
    case CreateGroupSet(gs) => GroupModel.createGroupSet(a, gs).map { wp => ReturnGroupSet(wp.item) }
    case CreateGroupsFromCsv(set, csv) => {
      // Do the import
      val rm = GroupModel.importFromCsv(a, set, csv)

      // Pull the group data back out to verify the import
      val data = for {
        // Group the registrations by group (Group.reg.target)
        registrations <- rm.collect
        (gId, regs) <- registrations.groupBy(_.target).toSeq.toRefMany

        // Look up the users' display names and put them with the group
        names <- {
          for {
            group <- gId.lazily
            ids <- regs.map(_.user).toRefMany.toIds
            u <- UserModel.findMany(a, ids).map(UserModel.displayName).collect
          } yield (group, u)
        }
      } yield names

      data.collect.map(ReturnGroupsData.apply)
    }

  }

}
