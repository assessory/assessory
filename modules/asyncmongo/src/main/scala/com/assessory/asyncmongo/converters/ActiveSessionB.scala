package com.assessory.asyncmongo.converters

import com.assessory.api.appbase.ActiveSession
import org.mongodb.scala.bson._

import scala.util.Try

object ActiveSessionB  {
  def write(i: ActiveSession) = Document(
    "ip" -> i.ip,
    "key" -> i.key,
    "since" -> i.since
  )

  def read(doc: Document): Try[ActiveSession] = Try {
    new ActiveSession(
      ip = doc[BsonString]("ip").getValue,
      key  = doc[BsonString]("key").getValue,
      since = doc[BsonInt64]("since").getValue
    )
  }
}
