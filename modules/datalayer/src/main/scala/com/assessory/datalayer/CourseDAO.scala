package com.assessory.datalayer

import com.assessory.api.appbase.{ActiveSession, Course, Identity, PasswordLogin, User, UserId}

import com.assessory.api.wiring.Lookups
import com.assessory.api.Task
import com.wbillingsley.handy.{Ref, RefOpt, RefMany}

trait CourseDAO {

    def byShortName(sn:String): RefMany[Course]

    def saveNew(c:Course):Ref[Course]


}