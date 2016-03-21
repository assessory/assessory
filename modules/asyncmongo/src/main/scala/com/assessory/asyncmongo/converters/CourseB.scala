package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{LTIConsumer, Course}
import org.mongodb.scala.bson._
import scala.collection.JavaConverters._

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
    "ltis" -> i.ltis.map(LTIB.write),
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
      ltis = doc[BsonArray]("ltis").getValues.asScala.map({ d => LTIB.read(Document(d.asDocument())).get }),
      created = doc[BsonInt64]("created")
    )
  }
}

object LTIB {
  def write(l:LTIConsumer) = Document(
    "clientKey" -> l.clientKey,
    "secret" -> l.secret,
    "comment" -> l.comment
  )

  def read(doc:Document) = Try {
    new LTIConsumer(
      clientKey = doc[BsonString]("clientKey"),
      secret = doc[BsonString]("secret"),
      comment = doc.get[BsonString]("comment")
    )
  }
}