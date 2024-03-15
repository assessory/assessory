package com.assessory.datalayer

import com.assessory.api.appbase.{ActiveSession, Course, Identity, PasswordLogin, User, UserId}

import com.assessory.api.wiring.Lookups
import com.assessory.api.Task
import com.wbillingsley.handy.{Ref, RefOpt, RefMany}

trait TaskDAO {

    def saveSafe(c:Task):Ref[Task]

    def updateBody(t:Task):RefOpt[Task]

    def byName(c:Ref[Course], n:String):RefOpt[Task]

    def byCourse(c:Ref[Course]):RefMany[Task]

}