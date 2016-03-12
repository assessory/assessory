package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.appbase._
import com.wbillingsley.handy.{EmptyKind, HasKind, Id}
import org.mongodb.scala.bson._
import BsonHelpers._
import scala.collection.JavaConverters._

import scala.util.Try

class RegistrationB[T, R, P <: HasKind](val rToFromBson:ToFromBson[R], val pToFromBson:ToFromBson[P]) {
  def write(i: Registration[T, R, P]) = Document(
    "_id" -> IdB.write(i.id),
    "user" -> IdB.write(i.user),
    "target" -> IdB.write(i.target),
    "roles" -> i.roles.toSeq.map { r => rToFromBson.toBson(r) },
    "provenance" -> pToFromBson.toBson(i.provenance),
    "updated" -> System.currentTimeMillis(),
    "created" -> i.created
  )

  def read(doc: Document): Try[Registration[T, R, P]] = Try {
    new Registration[T, R, P](
      id = doc[BsonObjectId]("_id"),
      user = doc[BsonObjectId]("user"),
      target = doc[BsonObjectId]("target"),
      roles = doc[BsonArray]("roles").iterator().asScala.map({ b => rToFromBson.fromBson(b) }).toSet,
      provenance = pToFromBson.fromBson(doc[BsonValue]("provenance")),
      updated = doc[BsonInt64]("updated"),
      created = doc[BsonInt64]("created")
    )
  }
}

object RegistrationB {

  implicit object CourseRoleToFromBson extends ToFromBson[CourseRole] {
    override def fromBson(b: BsonValue): CourseRole = CourseRole(b.asString.getValue)
    override def toBson(i: CourseRole): BsonValue = new BsonString(i.r)
  }

  implicit object GroupRoleToFromBson extends ToFromBson[GroupRole] {
    override def fromBson(b: BsonValue): GroupRole = GroupRole(b.asString.getValue)
    override def toBson(i: GroupRole): BsonValue = new BsonString(i.r)
  }

  implicit object EmptyKindToFromBson extends ToFromBson[HasKind] {
    override def fromBson(b: BsonValue) = EmptyKind
    override def toBson(i: HasKind): BsonValue = Document("kind" -> new BsonString("empty")).toBsonDocument
  }

  val courseRegB = new RegistrationB[Course, CourseRole, HasKind](CourseRoleToFromBson, EmptyKindToFromBson)
  val groupRegB = new RegistrationB[Group, GroupRole, HasKind](GroupRoleToFromBson, EmptyKindToFromBson)
}
