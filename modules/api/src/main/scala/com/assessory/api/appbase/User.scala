package com.assessory.api.appbase

import com.wbillingsley.handy.{HasId, Id}

trait UserT[U, I <: IdentityT, PL <: PasswordLoginT] {

  val id:Id[U,String]

  val name: Option[String]

  val nickname: Option[String]

  val pwlogin: PL

  val identities: Seq[I]

  val avatar: Option[String]

  val created: Long

}

object User {

  def defaultCreated = System.currentTimeMillis

}



case class User(

  id:UserId,

  name:Option[String] = None,

  nickname:Option[String] = None,

  avatar:Option[String] = None,

  pwlogin: PasswordLogin = PasswordLogin(),

  secret: String = scala.util.Random.alphanumeric.take(16).mkString,

  identities:Seq[Identity] = Seq.empty,

  activeSessions:Seq[ActiveSession] = Seq.empty,

  created: Long = User.defaultCreated

) extends UserT[User, Identity, PasswordLogin] with HasId[UserId] {

  def getIdentity(service:String) = identities.find(p => p.service == service)

}

case class UserId(id:String) extends Id[User, String]