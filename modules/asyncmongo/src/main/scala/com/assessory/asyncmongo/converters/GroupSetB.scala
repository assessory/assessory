package com.assessory.asyncmongo.converters


import com.wbillingsley.handy.appbase.{Course, GroupSet}
import org.mongodb.scala.bson._

import scala.util.Try

object GroupSetB {
  def write(i: GroupSet) = Document(
    "_id" -> IdB.write(i.id),
    "course" -> IdB.write(i.course),
    "name" -> i.name,
    "description" -> i.description,
    "parent" -> IdB.write(i.parent),
    "created" -> i.created
  )

  def read(doc: Document): Try[GroupSet] = Try {
    new GroupSet(
      id = doc[BsonObjectId]("_id"),
      course = doc[BsonObjectId]("course"),
      name = doc.get[BsonString]("name"),
      description = doc.get[BsonString]("description"),
      parent = doc.get[BsonObjectId]("parent"),
      created = doc[BsonInt64]("created")
    )
  }
}
