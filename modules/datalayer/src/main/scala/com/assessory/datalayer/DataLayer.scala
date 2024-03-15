
package com.assessory.datalayer

import com.assessory.api.appbase.{ActiveSession, Course, Identity, PasswordLogin, User, UserId}

import com.assessory.api.wiring.Lookups
import com.assessory.api.appbase.User
import com.wbillingsley.handy.RefOpt
import com.wbillingsley.handy.Ref

trait DataLayer {

    given lookups:Lookups.type

    given userDAO:UserDAO

    given taskDAO:TaskDAO

    given courseDAO:CourseDAO

}

