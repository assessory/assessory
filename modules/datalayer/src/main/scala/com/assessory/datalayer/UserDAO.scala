package com.assessory.datalayer

import com.assessory.api.appbase.{ActiveSession, Course, Identity, PasswordLogin, User, UserId}

import com.assessory.api.wiring.Lookups
import com.assessory.api.appbase.User
import com.wbillingsley.handy.RefOpt
import com.wbillingsley.handy.Ref


trait UserDAO  {
    def unsaved: User

    def saveSafe(c:User):Ref[User]

    def saveNew(c:User):Ref[User]

    def saveDetails(u:User):RefOpt[User]

    def byEmail(email:String):RefOpt[User]

    def byEmailAndPassword(email:String, password:String):RefOpt[User]

    def byUsernameAndPassword(username:String, password:String):RefOpt[User]

    def bySocialIdOrUsername(service:String, optId:Option[String], optUserName:Option[String] = None):RefOpt[User]

    /**
     * Get the user by their current session identifier
     */
    def bySessionKey(sessionKey:String):RefOpt[User]

    /**
     * By a social login, or other identifier in another service
     */
    def byIdentity(identity:Identity):RefOpt[User]

    /**
     * Push a new session into the user's sessions.
     * After this call, a request to <code>bySessionKey</code> for the session key should retrieve this user.
     */
    def addSession(user:Ref[User], session:ActiveSession):Ref[User]

    /**
     * Remove a session from the user's sessions.
     * After this call, a request to <code>bySessionKey</code> for the session key should not retrieve this user.
     */
    def removeSession(user:Ref[User], sessionKey:String):Ref[User]

    /**
     * Add a social identity to the user's identities
     * After this call, a request to <code>byIdentity</code> for this identity should not retrieve this user.
     */
    def addIdentity(user:Ref[User], identity:Identity):Ref[User]

    /**
     * Remove a social identity from the user's sessions.
     * After this call, a request to <code>byIdentity</code> for this identity should should not retrieve this user.
     */
    def removeIdentity(user:Ref[User], identity:Identity):Ref[User]

    /**
     * Generate a salt and hash for this password
     */
    def hash(password: String):String

    /**
     * Check if the given password matches the given password login
     */
    def checkPassword(pwlogin:PasswordLogin, pw:String):Boolean    

}