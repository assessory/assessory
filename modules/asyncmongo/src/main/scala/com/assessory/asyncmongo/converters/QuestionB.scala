package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.Id
import com.assessory.api.question._
import org.mongodb.scala.bson._

import scala.util.{Failure, Try}

object QuestionB {
  def write(i: Question) = i match {
    case ShortTextQuestion(id, prompt, maxLength, hideInCrit) => Document("kind" -> "Short Text", "_id" -> IdB.write(id), "prompt" -> prompt, "maxLength" -> maxLength, "hideInCrit" -> hideInCrit)
    case BooleanQuestion(id, prompt, hideInCrit) => Document("kind" -> "Boolean", "_id" -> IdB.write(id), "prompt" -> prompt, "hideInCrit" -> hideInCrit)
    case VideoQuestion(id, prompt, hideInCrit) => Document("kind" -> "Video", "_id" -> IdB.write(id), "prompt" -> prompt, "hideInCrit" -> hideInCrit)
    case FileQuestion(id, prompt, hideInCrit) => Document("kind" -> "File", "_id" -> IdB.write(id), "prompt" -> prompt, "hideInCrit" -> hideInCrit)
  }

  def read(doc: Document): Try[Question] = Try {

    doc[BsonString]("kind").getValue match {
      case "Short Text" => ShortTextQuestion(
        id = doc[BsonObjectId]("_id"),
        prompt = doc[BsonString]("prompt"),
        maxLength = doc.get[BsonInt32]("maxLength"),
        hideInCrit = doc.get[BsonBoolean]("hideInCrit").map(_.getValue).getOrElse(false)
      )
      case "Boolean" => BooleanQuestion(
        id = doc[BsonObjectId]("_id"),
        prompt = doc[BsonString]("prompt"),
        hideInCrit = doc.get[BsonBoolean]("hideInCrit").map(_.getValue).getOrElse(true)
      )
      case "Video" => VideoQuestion(
        id = doc[BsonObjectId]("_id"),
        prompt = doc[BsonString]("prompt"),
        hideInCrit = doc.get[BsonBoolean]("hideInCrit").map(_.getValue).getOrElse(false)
      )
      case "File" => FileQuestion(
        id = doc[BsonObjectId]("_id"),
        prompt = doc[BsonString]("prompt"),
        hideInCrit = doc.get[BsonBoolean]("hideInCrit").map(_.getValue).getOrElse(false)
      )
    }
  }
}

object AnswerB {
  def write(i: Answer) = i match {
    case ShortTextAnswer(question, answer) =>
      Document("kind" -> "Short Text", "question" -> IdB.write(question), "answer" -> answer)
    case BooleanAnswer(question,answer) =>
      Document("kind" -> "Boolean", "question" -> IdB.write(question), "answer" -> answer)
    case VideoAnswer(question, answer) =>
      Document("kind" -> "Video", "question" -> IdB.write(question), "answer" -> answer.map(VideoResourceB.write))
    case FileAnswer(question, answer) =>
      Document("kind" -> "SmallFile", "question" -> IdB.write(question), "answer" -> IdB.write(answer))
  }

  def read(doc: Document): Try[Answer] = Try {
    doc[BsonString]("kind").getValue match {
      case "Short Text" => ShortTextAnswer(
        question = doc[BsonObjectId]("question"),
        answer = doc.get[BsonString]("answer")
      )
      case "Boolean" => BooleanAnswer(
        question = doc[BsonObjectId]("question"),
        answer = doc.get[BsonBoolean]("answer")
      )
      case "Video" => VideoAnswer(
        question = doc[BsonObjectId]("question"),
        answer = doc.get[BsonDocument]("answer").map({ d => VideoResourceB.read(Document(d)).get })
      )
      case "SmallFile" => FileAnswer(
        question = doc[BsonObjectId]("question"),
        answer = doc.get[BsonObjectId]("answer")
      )
    }
  }
}
