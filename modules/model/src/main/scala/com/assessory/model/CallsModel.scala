package com.assessory.model

import com.assessory.api.call._
import com.assessory.asyncmongo.UserDAO
import com.wbillingsley.handy.{Approval, Ref, RefFailed, refOps, Id, lazily}
import com.assessory.api.appbase.{ActiveSession, User}
import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups.given

object CallsModel {

  def call(a:Approval[User], c:Call):Ref[Return] = c match {
      
    // Session commands
    case SessionCall.WithSession(a, c) => call(Approval(UserDAO.bySessionKey(a.key)), c)
    case SessionCall.Register(email, password, session) => UserModel.signUp(Some(email), Some(password), session).map(ReturnUser.apply)
    case SessionCall.Login(email, password, session) => UserModel.logIn(Some(email), Some(password), session).map(ReturnUser.apply).require
    case SessionCall.Logout(session) => UserModel.logOut(a.who.require, session).map(ReturnUser.apply)

    case UserCall.WhoAmI => (for u <- a.who yield ReturnUser(u)).orElse(StandardReturn.ReturnNone.itself)
    case UserCall.GetUser(id) => (for u <- id.lazily yield ReturnUser(u)) // TODO: Expurgated users

    case CourseCall.CreateCourse(c) => CourseModel.create(a, c).map {
      wp => StandardReturn.ReturnWithPermissions(ReturnCourse(wp.item), wp.perms)
    }

    case CourseCall.GetCourse(id) => CourseModel.byId(a, id).map {
      wp => StandardReturn.ReturnWithPermissions(ReturnCourse(wp.item), wp.perms)
    }

    case CourseCall.MyCourses => CourseModel.myCourses(a).map(wp =>
      StandardReturn.ReturnWithPermissions(ReturnCourse(wp.item), wp.perms)
    ).collect.map(s => StandardReturn.ReturnMany(s))

    case tc:TaskCall => TaskModel.handleCall(a, tc)
    case toc:TaskOutputCall => TaskOutputModel.handleCall(a, toc)
    case gc:GroupSetCall => GroupModel.handleGroupSetCall(a, gc)
    case gc:GroupCall => GroupModel.handleGroupCall(a, gc)
    case cc:CritiqueCall => CritModel.handleCall(a, cc)
  }

}
