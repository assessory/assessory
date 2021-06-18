package com.assessory.asyncmongo.converters

import com.assessory.api.appbase.{Course, CourseId, Group, GroupId, GroupSet, GroupSetId, RegistrationId}
import com.assessory.asyncmongo.helpers._
import com.wbillingsley.handy.{Id, Ids}
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
    "members" -> i.members.write,
    "created" -> i.created
  )

  def read(doc: Document): Try[Group] = Try {
    new Group(
      id = GroupId(doc.hexOid("_id")),
      course = doc.optHexOid("course").map(CourseId(_)),
      set = GroupSetId(doc.hexOid("set")),
      parent = doc.optHexOid("parent").map(GroupId(_)),
      name = doc.optString("name"),
      provenance = doc.optString("provenance"),
      members = doc.seqHexOid("members").map(RegistrationId(_)),
      created = doc.long("created")
    )
  }
}
