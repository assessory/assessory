package com.assessory.asyncmongo.converters

import com.assessory.api.critique._
import com.assessory.api.question.QuestionnaireTaskOutput
import com.assessory.api.video._
import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{User, Group, Course}
import com.assessory.api._
import org.mongodb.scala.bson._
import scala.jdk.CollectionConverters.*


import scala.util.{Failure, Try}

object TaskRecordB  {

  def rTOB(doc:BsonDocument):TaskOutputBody = TaskOutputBodyB.read(Document(doc)).get
  def rTarget(doc:BsonDocument):Target = TargetB.read(Document(doc)).get
  def rBy(doc:BsonDocument):By = ByB.read(Document(doc)).get

  def write(r: TaskRecord) = Document(
    "_id" -> IdB.write(r.id),
    "task" -> IdB.write(r.task),
    "by" -> ByB.write(r.by),
    "outputs" -> r.outputs.map(IdB.write)
  )

  def read(doc: Document): Try[TaskRecord] = Try {
    new TaskRecord(
      id = TaskRecordId(doc[BsonObjectId]("_id").getValue.toHexString),
      task = TaskId(doc[BsonObjectId]("task").getValue.toHexString),
      by = rBy(doc[BsonDocument]("by")),
      outputs = doc[BsonArray]("outputs").getValues.asScala.map({ case d => TaskOutputId(d.asObjectId().getValue().toHexString) }).toSeq,
    )
  }
}
