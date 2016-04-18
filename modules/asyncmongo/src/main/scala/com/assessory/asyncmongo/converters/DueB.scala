package com.assessory.asyncmongo.converters

import com.assessory.api.due._
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.Group

import org.mongodb.scala.bson._

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}


object DueB {
  def write(i: Due) = i match {
    case DueDate(time) => Document("kind" -> "Date", "time" -> time)
    case DuePerGroup(times) => Document("kind" -> "Per Group", "times" -> times.toSeq.map { case (id, t) => Document("group" -> IdB.write(id), "time" -> t) })
    case NoDue => Document("kind" -> "No due date")
  }

  def read(doc: Document): Try[Due] = Try {
    doc[BsonString]("kind").getValue match {
      case "Date" => DueDate(time = doc[BsonInt64]("time"))
      case "Per Group" => {
        val times = {
          val arr = doc[BsonArray]("times").getValues.asScala
          for (entry <- arr) yield {
            val e = entry.asDocument()
            IdB.read[Group](e.get("group").asObjectId()) -> doc[BsonInt64]("time").getValue
          }
        }.toMap

        DuePerGroup(times=times)
      }
      case "No due date" => NoDue
    }
  }
}
