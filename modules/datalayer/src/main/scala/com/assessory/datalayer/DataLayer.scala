
package com.assessory.datalayer

import com.assessory.api.appbase.{ActiveSession, Course, Identity, PasswordLogin, User, UserId}

import com.assessory.api.wiring.Lookups
import com.assessory.api.appbase.User
import com.wbillingsley.handy.RefOpt
import com.wbillingsley.handy.Ref

trait DataLayer {

    given lookups:Lookups.type

}

trait UserDAO extends com.assessory.api.appbase.UserDAO[User, Identity] {
    def unsaved: User

    def saveSafe(c:User):Ref[User]

    def saveNew(c:User):Ref[User]

    def saveDetails(u:User):RefOpt[User]

    def byEmail(email:String):RefOpt[User]

    def byEmailAndPassword(email:String, password:String):RefOpt[User]

    def byUsernameAndPassword(username:String, password:String):RefOpt[User]

    def bySocialIdOrUsername(service:String, optId:Option[String], optUserName:Option[String] = None):RefOpt[User]

}