package com.assessory.asyncmongo.converters

import com.assessory.api.appbase.Identity
import org.mongodb.scala.bson._
import com.assessory.asyncmongo.converters._

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
      service = doc.string("service"),
      value = doc.optString("value"),
      username = doc.optString("username"),
      avatar = doc.optString("avatar"),
      since = doc.long("since")
    )
  }
}
