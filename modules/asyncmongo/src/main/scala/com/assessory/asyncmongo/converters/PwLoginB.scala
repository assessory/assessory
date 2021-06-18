package com.assessory.asyncmongo.converters

import com.assessory.api.appbase.PasswordLogin
import org.mongodb.scala.bson._
import com.assessory.asyncmongo.converters._

import scala.util.Try

object PwLoginB  {

  def write(i: PasswordLogin) = Document(
    "email" -> i.email,
    "pwhash" -> i.pwhash,
    "username" -> i.username
  )

  def read(doc: Document): Try[PasswordLogin] = Try {
    new PasswordLogin(
      email = doc.optString("email"),
      username  = doc.optString("username"),
      pwhash = doc.optString("pwhash")
    )
  }
}
