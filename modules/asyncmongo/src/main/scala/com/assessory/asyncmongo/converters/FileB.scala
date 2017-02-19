package com.assessory.asyncmongo.converters


import com.assessory.api.question.QuestionnaireTask
import com.assessory.api.video._
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{GroupSet}
import com.assessory.api._
import critique._
import question._
import org.mongodb.scala.bson._
import scala.collection.JavaConverters._


import scala.util.{Success, Failure, Try}

object SmallFileB {
  def write(i: SmallFile):Document = Document(
    "_id" -> IdB.write(i.id),
    "details" -> SmallFileDetailsB.write(i.details),
    "data" -> BsonBinary(i.data)
  )

  def read(doc: Document): Try[SmallFile] = Try {
    SmallFile(
      id = IdB.read(doc[BsonObjectId]("_id")),
      details = SmallFileDetailsB.read(Document(doc[BsonDocument]("details"))).get,
      data = doc.get[BsonBinary]("data").map(_.getData).getOrElse(Array.empty)
    )
  }
}

object SmallFileDetailsB {
  def write(i: SmallFileDetails):Document = Document(
    "_id" -> IdB.write(i.id),
    "course" -> IdB.write(i.courseId),
    "owner" -> IdB.write(i.ownerId),
    "name" -> i.name,
    "size" -> i.size,
    "mime" -> i.mime,
    "created" -> i.created,
    "updated" -> i.updated
  )

  def read(doc: Document): Try[SmallFileDetails] = Try {
    SmallFileDetails(
      id = IdB.read(doc[BsonObjectId]("_id")),
      courseId = IdB.read(doc[BsonObjectId]("course")),
      ownerId = IdB.read(doc[BsonObjectId]("owner")),
      name = doc[BsonString]("name"),
      size = doc.get[BsonInt64]("size"),
      mime = doc.get[BsonString]("mime"),
      created = doc[BsonInt64]("created"),
      updated = doc[BsonInt64]("updated")
    )
  }
}