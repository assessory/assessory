package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.Course
import org.mongodb.scala.bson._


import scala.util.Try

object CourseB  {

  def write(i: Course) = Document(
    "_id" -> IdB.write(i.id),
    "title" -> i.title,
    "shortName" -> i.shortName,
    "shortDescription" -> i.shortDescription,
    "website" -> i.website,
    "coverImage" -> i.coverImage,
    "addedBy" -> IdB.write(i.addedBy),
    "secret" -> i.secret,
    "created" -> i.created
  )

  def read(doc: Document): Try[Course] = Try {
    new Course(
      id = doc[BsonObjectId]("_id"),
      title = doc.get[BsonString]("title"),
      shortName = doc.get[BsonString]("shortName"),
      shortDescription = doc.get[BsonString]("shortDescription"),
      website = doc.get[BsonString]("website"),
      coverImage = doc.get[BsonString]("coverImage"),
      addedBy = doc[BsonObjectId]("addedBy"),
      secret = doc[BsonString]("secret"),
      created = doc[BsonInt64]("created")
    )
  }
}
