package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase._
import org.mongodb.scala.bson._

import scala.util.{Failure, Try}

object QuestionB {
  def write(i: Question) = i match {
    case ShortTextQuestion(id, prompt, maxLength) => Document("kind" -> "Short Text", "_id" -> IdB.write(id), "prompt" -> prompt, "maxLength" -> maxLength)
    case BooleanQuestion(id, prompt) => Document("kind" -> "Boolean", "_id" -> IdB.write(id), "prompt" -> prompt)
  }

  def read(doc: Document): Try[Question] = Try {

    doc[BsonString]("kind").getValue match {
      case "Short Text" => ShortTextQuestion(
        id = doc[BsonObjectId]("_id"),
        prompt = doc[BsonString]("prompt"),
        maxLength = doc.get[BsonInt32]("maxLength")
      )
      case "Boolean" => BooleanQuestion(
        id = doc[BsonObjectId]("_id"),
        prompt = doc[BsonString]("prompt")
      )
    }
  }
}

object AnswerB {
  def write(i: Answer[_]) = i match {
    case ShortTextAnswer(question, answer) =>
      Document("kind" -> "Short Text", "question" -> IdB.write(question), "answer" -> answer)
    case BooleanAnswer(question,answer) =>
      Document("kind" -> "Boolean", "question" -> IdB.write(question), "answer" -> answer)
  }

  def read(doc: Document): Try[Answer[_]] = Try {
    doc[BsonString]("kind").getValue match {
      case "Short Text" => ShortTextAnswer(
        question = doc[BsonObjectId]("question"),
        answer = doc.get[BsonString]("answer")
      )
      case "Boolean" => BooleanAnswer(
        question = doc[BsonObjectId]("question"),
        answer = doc.get[BsonBoolean]("answer")
      )
    }
  }
}
