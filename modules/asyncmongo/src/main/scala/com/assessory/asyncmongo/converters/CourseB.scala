package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{Course, CourseId, LTIConsumer, RegistrationId}
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
      id = CourseId(doc.hexOid("_id")),
      title = doc.optString("title"),
      shortName = doc.optString("shortName"),
      shortDescription = doc.optString("shortDescription"),
      website = doc.optString("website"),
      coverImage = doc.optString("coverImage"),
      addedBy = RegistrationId(doc.hexOid("addedBy")),
      secret = doc.string("secret"),
      ltis = doc[BsonArray]("ltis").getValues.asScala.map({ d => LTIB.read(Document(d.asDocument())).get }).toSeq,
      created = doc.long("created")
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
      clientKey = doc.string("clientKey"),
      secret = doc.string("secret"),
      comment = doc.optString("comment")
    )
  }
}