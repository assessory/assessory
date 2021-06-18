package com.assessory.asyncmongo.converters

import com.assessory.api.due._
import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{Group, GroupId}
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
      case "Date" => DueDate(time = doc.long("time"))
      case "Per Group" => {
        val times:Map[Id[Group, String], Long] = {
          val arr = doc[BsonArray]("times")
          for (entry <- arr.getValues.asScala.map(_.asDocument())) yield {
            GroupId(entry.hexOid("group")) -> doc.long("time")
          }
        }.toMap

        DuePerGroup(times=times)
      }
      case "No due date" => NoDue
    }
  }
}
