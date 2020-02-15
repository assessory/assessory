package com.assessory.asyncmongo.converters


import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.RegistrationB._
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase._
import org.mongodb.scala.bson._
import scala.collection.JavaConverters._

import scala.util.Try

class PreenrolmentB[W, T,R,RT](implicit r:ToFromBson[R]) {

  implicit val prb = new PreenrolmentRowB[T,R,RT]

  def write(i: Preenrolment[W,T,R,RT]) = Document(
    "_id" -> IdB.write(i.id),
    "name" -> i.name,
    "rows" -> BsonArray(i.rows.map({ r => prb.write(r) })),
    "modified" -> i.modified,
    "created" -> i.created
  )

  def read(doc: Document): Try[Preenrolment[W,T,R,RT]] = Try {
    new Preenrolment[W,T,R,RT](
      id = doc[BsonObjectId]("_id"),
      name = doc.get[BsonString]("name"),
      rows = doc[BsonArray]("rows").getValues.asScala.map({ r => prb.read(Document(r.asDocument())).get }).toSeq,
      modified = doc[BsonInt64]("modified"),
      created = doc[BsonInt64]("created")
    )
  }
}

object PreenrolmentB {
  implicit val group:PreenrolmentB[GroupSet, Group, GroupRole, Group.Reg] = new PreenrolmentB
  implicit val course:PreenrolmentB[Course, Course, CourseRole, Course.Reg] = new PreenrolmentB
}

class PreenrolmentRowB[T,R,UT](implicit r:ToFromBson[R]) {

  def write(i: Preenrolment.Row[T,R,UT]) = bsonDoc(
    "target" -> IdB.write(i.target),
    "identity" -> IdentityLookupB.write(i.identity),
    "roles" -> i.roles.toSeq,
    "used" -> i.used.map(UsedB.write).getOrElse(BsonNull())
  )

  def read(doc: Document): Try[Preenrolment.Row[T,R,UT]] = Try {
    new Preenrolment.Row[T,R,UT](
      target = doc[BsonObjectId]("target"),
      identity = IdentityLookupB.read(Document.apply(doc[BsonDocument]("identity"))).get,
      roles = doc[BsonArray]("roles").getValues.asScala.map({ x => r.fromBson(x)}).toSet,
      used = doc.get[BsonDocument]("used").map({ d => UsedB.read[UT](Document(d)).get})
    )
  }
}

object IdentityLookupB  {

  def write(i: IdentityLookup) = bsonDoc(
    "service" -> i.service,
    "username" -> i.username,
    "value" -> i.value
  )

  def read(doc: Document): Try[IdentityLookup] = Try {
    new IdentityLookup(
      service = doc[BsonString]("service"),
      username = doc.get[BsonString]("username"),
      value = doc.get[BsonString]("value")
    )
  }
}

object UsedB  {
  def write[UT](i: Used[UT]) = bsonDoc(
    "target" -> IdB.write(i.target),
    "time" -> i.time
  )

  def read[UT](doc: Document): Try[Used[UT]] = Try {
    new Used[UT](
      target = doc[BsonObjectId]("target"),
      time = doc[BsonInt64]("time")
    )
  }
}


