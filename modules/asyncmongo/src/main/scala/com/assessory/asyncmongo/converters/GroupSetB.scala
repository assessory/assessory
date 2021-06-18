package com.assessory.asyncmongo.converters


import com.assessory.api.appbase.{Course, CourseId, GroupSet, GroupSetId}
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
      id = GroupSetId(doc.hexOid("_id")),
      course = CourseId(doc.hexOid("course")),
      name = doc.optString("name"),
      description = doc.optString("description"),
      parent = doc.optHexOid("parent").map(GroupSetId(_)),
      created = doc.long("created")
    )
  }
}
