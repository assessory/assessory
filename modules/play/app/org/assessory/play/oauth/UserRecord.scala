package org.assessory.play.oauth

import play.api.libs.json.{Json, JsValue}

case class AuthFailed(msg:String) extends Exception(msg)

object AuthFailed {
  val DECLINED = AuthFailed("No user was authorized")
}


  /**
  * Remembers details about a user from a log-in service if that user has not logged in before.
  * (So that we can show the "register new account?" form)
  */
case class UserRecord (
                        service: String,
                        id: String,
                        username: Option[String],
                        name: Option[String],
                        nickname: Option[String],
                        avatar: Option[String],
                        raw: Option[JsValue] = None
                      ) {

  def toJsonString = UserRecord.jsonFormat.writes(UserRecord.this).toString
}

object UserRecord {

  implicit val jsonFormat = Json.format[UserRecord]

}

trait Token

trait Details

case class OAuthDetails(
                         userRecord: UserRecord,
                         raw: Option[JsValue],
                         returnTo: Option[String] = None,
                         details: Option[Details] = None
                       ) {

  def toJson = Json.obj(
    "userRecord" -> UserRecord.jsonFormat.writes(userRecord),
    "returnTo" -> returnTo,
    "raw" -> raw
  )

}