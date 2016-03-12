package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.appbase.Identity
import org.mongodb.scala.bson._

import scala.util.Try

object IdentityB {
  def write(i: Identity) = Document(
    "service" -> i.service,
    "username" -> i.username,
    "value" -> i.value,
    "avatar" -> i.avatar,
    "since" -> i.since
  )

  def read(doc: Document): Try[Identity] = Try {
    new Identity(
      service = doc[BsonString]("service"),
      value = doc.get[BsonString]("value"),
      username = doc.get[BsonString]("username"),
      avatar = doc.get[BsonString]("avatar"),
      since = doc[BsonInt64]("since")
    )
  }
}
