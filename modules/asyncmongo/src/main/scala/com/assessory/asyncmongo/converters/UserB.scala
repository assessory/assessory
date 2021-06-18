package com.assessory.asyncmongo.converters


import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{ActiveSession, Identity, PasswordLogin, User, UserId}
import org.mongodb.scala.bson._

import scala.collection.JavaConverters._
import scala.util.Try

object UserB {
  def write(i: User) = Document(
    "_id" -> IdB.write(i.id),
    "name" -> i.name,
    "nickname" -> i.nickname,
    "avatar" -> i.avatar,
    "secret" -> i.secret,
    "activeSessions" -> i.activeSessions.map(ActiveSessionB.write),
    "pwlogin" -> PwLoginB.write(i.pwlogin),
    "identities" -> i.identities.map(IdentityB.write),
    "created" -> i.created
  )

  def read(doc: Document): Try[User] = Try {
    new User(
      id = UserId(doc.hexOid("_id")),
      name  = doc.optString("name"),
      nickname = doc.optString("nickname"),
      avatar = doc.optString("avatar"),
      secret = doc.string("secret"),
      activeSessions = doc[BsonArray]("activeSessions").getValues.asScala.map({ case x => ActiveSessionB.read(Document(x.asDocument())).get }).toSeq,
      pwlogin = PwLoginB.read(Document(doc[BsonDocument]("pwlogin"))).get,
      identities = doc[BsonArray]("identities").getValues.asScala.map({ case x => IdentityB.read(Document(x.asDocument())).get }).toSeq,
      created = doc.long("created")
    )
  }
}
