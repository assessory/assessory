package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.appbase.PasswordLogin
import org.mongodb.scala.bson._

import scala.util.Try

object PwLoginB  {

  def write(i: PasswordLogin) = Document(
    "email" -> i.email,
    "pwhash" -> i.pwhash,
    "username" -> i.username
  )

  def read(doc: Document): Try[PasswordLogin] = Try {
    new PasswordLogin(
      email = doc.get[BsonString]("email"),
      username  = doc.get[BsonString]("username"),
      pwhash = doc.get[BsonString]("pwhash")
    )
  }
}
