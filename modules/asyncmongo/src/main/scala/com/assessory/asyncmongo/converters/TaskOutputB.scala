package com.assessory.asyncmongo.converters

import com.assessory.api.critique._
import com.assessory.api.question.QuestionnaireTaskOutput
import com.assessory.api.video._
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
          "video" -> v.video.map({ v => VideoResourceB.write(v) })
        )
      case m:MessageTaskOutput => MessageTaskOutputBodyB.write(m)
      case c:CompositeTaskOutput => CompositeTaskOutputBodyB.write(c)
      case s:SmallFileTaskOutput => SmallFileTaskOutputBodyB.write(s)
    }
  }

  def read(doc: Document): Try[TaskOutputBody] = {
    doc[BsonString]("kind").getValue match {
      case "Critique" => Try { Critique(
        target = TargetB.read(Document(doc[BsonDocument]("target"))).get,
        task = read(Document(doc[BsonDocument]("task"))).get
      ) }
      case "Questionnaire" => Try { QuestionnaireTaskOutput(
        answers = doc[BsonArray]("answers").getValues.asScala.map({ case x => AnswerB.read(Document(x.asDocument())).get })
      ) }
      case "Video" => Try { VideoTaskOutput(
        video = doc.get[BsonDocument]("video").map({ d => VideoResourceB.read(Document(d)).get })
      ) }
      case "Composite" => CompositeTaskOutputBodyB.read(doc)
      case "Message" => MessageTaskOutputBodyB.read(doc)
      case "SmallFile" => SmallFileTaskOutputBodyB.read(doc)
    }
  }
}

object VideoResourceB {
  def write(i:VideoResource) = i match {
    case YouTube(ytId) => Document("kind" -> "YouTube", "youtubeId" -> ytId)
    case Kaltura(kId) => Document("kind" -> "Kaltura", "kalturaId" -> kId)
    case UnrecognisedVideoUrl(url) => Document("kind" -> "URL", "url" -> url)
  }

  def read(doc:Document): Try[VideoResource] = Try {
    doc[BsonString]("kind").getValue match {
      case "YouTube" => YouTube(doc[BsonString]("youtubeId"))
      case "Kaltura" => Kaltura(doc[BsonString]("kalturaId"))
      case "URL" => UnrecognisedVideoUrl(doc[BsonString]("url"))
    }
  }
}

object CompositeTaskOutputBodyB {
  def write(c: CompositeTaskOutput):Document = {
    Document("kind" -> "Composite", "outputs" -> c.outputs.map(TaskOutputBodyB.write))
  }

  def read(doc:Document): Try[CompositeTaskOutput] = Try {
    CompositeTaskOutput(
      outputs = doc[BsonArray]("outputs").getValues.asScala.map({ d => TaskOutputBodyB.read(Document(d.asDocument())).get })
    )
  }
}


object MessageTaskOutputBodyB {
  def write(c: MessageTaskOutput):Document = {
    Document("kind" -> "Message", "text" -> c.text)
  }

  def read(doc:Document): Try[MessageTaskOutput] = Try {
    MessageTaskOutput(
      text = doc[BsonString]("text")
    )
  }
}


object SmallFileTaskOutputBodyB {
  def write(c: SmallFileTaskOutput):Document = {
    Document("kind" -> "SmallFile", "fileId" -> IdB.write(c.file))
  }

  def read(doc:Document): Try[SmallFileTaskOutput] = Try {
    SmallFileTaskOutput(
      file = doc.get[BsonObjectId]("fileId")
    )
  }
}