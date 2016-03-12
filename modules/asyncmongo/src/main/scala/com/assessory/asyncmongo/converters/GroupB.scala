package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.appbase.{GroupSet, Course, Group}
import com.wbillingsley.handy.{Ids, Id}
import org.mongodb.scala.bson._

import scala.util.Try

object GroupB {
  def write(i: Group) = Document(
    "_id" -> IdB.write(i.id),
    "course" -> IdB.write(i.course),
    "set" -> IdB.write(i.set),
    "parent" -> IdB.write(i.parent),
    "name" -> i.name,
    "provenance" -> i.provenance,
    "members" -> IdB.write(i.members),
    "created" -> i.created
  )

  def read(doc: Document): Try[Group] = Try {
    new Group(
      id = doc[BsonObjectId]("_id"),
      course = doc.get[BsonObjectId]("course"),
      set = doc[BsonObjectId]("set"),
      parent = doc.get[BsonObjectId]("parent"),
      name = doc.get[BsonString]("name"),
      provenance = doc.get[BsonString]("provenance"),
      members = doc[BsonArray]("members"),
      created = doc[BsonInt64]("created")
    )
  }
}
