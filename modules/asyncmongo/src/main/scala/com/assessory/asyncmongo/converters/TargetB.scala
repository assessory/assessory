package com.assessory.asyncmongo.converters

import com.assessory.api._
import org.mongodb.scala.bson._

import scala.util.{Failure, Try}


object TargetB {

  def write(i: Target) = i match {
    case TargetUser(id) => Document("kind" -> "User", "id" -> IdB.write(id))
    case TargetCourseReg(id) => Document("kind" -> "CourseReg", "id" -> IdB.write(id))
    case TargetGroup(id) => Document("kind" -> "Group", "id" -> IdB.write(id))
    case TargetTaskOutput(id) => Document("kind" -> "TaskOutput", "id" -> IdB.write(id))
  }

  def read(doc: Document): Try[Target] = Try {
    doc[BsonString]("kind").getValue match {
      case "User" => TargetUser(doc[BsonObjectId]("id"))
      case "CourseReg" => TargetCourseReg(doc[BsonObjectId]("id"))
      case "Group" => TargetGroup(doc[BsonObjectId]("id"))
      case "TaskOutput" => TargetTaskOutput(doc[BsonObjectId]("id"))
    }
  }
}