package com.assessory.asyncmongo.converters

import com.assessory.api.critique._
import com.assessory.api.question.QuestionnaireTaskOutput
import com.assessory.api.video.VideoTaskOutput
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{User, Group, Course}
import com.assessory.api._
import org.mongodb.scala.bson._
import scala.collection.JavaConverters._


import scala.util.{Failure, Try}

object TaskOutputB  {

  def rTOB(doc:BsonDocument):TaskOutputBody = TaskOutputBodyB.read(Document(doc)).get
  def rTarget(doc:BsonDocument):Target = TargetB.read(Document(doc)).get

  def write(i: TaskOutput) = Document(
    "_id" -> IdB.write(i.id),
    "task" -> IdB.write(i.task),
    "by" -> TargetB.write(i.by),
    "attn" -> i.attn.map { case x => TargetB.write(x) },
    "body" -> TaskOutputBodyB.write(i.body),
    "created" -> i.created,
    "finalised" -> i.finalised
  )

  def read(doc: Document): Try[TaskOutput] = Try {
    new TaskOutput(
      id = doc[BsonObjectId]("_id"),
      task = doc[BsonObjectId]("task"),
      by = rTarget(doc[BsonDocument]("by")),
      attn = doc[BsonArray]("attn").getValues.asScala.map({ case d => rTarget(d.asDocument()) }).toSeq,
      body = TaskOutputBodyB.read(Document(doc[BsonDocument]("body"))).get,
      created = doc[BsonInt64]("created"),
      finalised = doc.get[BsonInt64]("finalised")
    )
  }
}

object TaskOutputBodyB {
  def write(i: TaskOutputBody):Document = {
    i match {
      case c:Critique =>
        Document("kind" -> "Critique", "target" -> TargetB.write(c.target),
          "task" -> write(c.task)
        )
      case q:QuestionnaireTaskOutput =>
        Document("kind" -> "Questionnaire",
          "answers" -> q.answers.map { a => AnswerB.write(a) }
        )
      case v:VideoTaskOutput =>
        Document("kind" -> "Video",
          "videoId" -> IdB.write(v.videoId)
        )
    }
  }

  def read(doc: Document): Try[TaskOutputBody] = Try {
    doc[BsonString]("kind").getValue match {
      case "Critique" => Critique(
        target = TargetB.read(Document(doc[BsonDocument]("target"))).get,
        task = read(Document(doc[BsonDocument]("task"))).get
      )
      case "Questionnaire" => QuestionnaireTaskOutput(
        answers = doc[BsonArray]("answers").getValues.asScala.map({ case x => AnswerB.read(Document(x.asDocument())).get })
      )
      case "Video" => VideoTaskOutput(
        videoId = doc.get[BsonObjectId]("videoId")
      )
    }
  }
}
