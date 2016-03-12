package com.assessory.asyncmongo

import com.assessory.asyncmongo.converters.{CourseB, IdB}
import com.wbillingsley.handy.appbase.Course
import com.wbillingsley.handy.Ref._
import org.mongodb.scala.model.FindOneAndReplaceOptions

import org.mongodb.scala.model.Filters._
import com.assessory.asyncmongo.converters.BsonHelpers._

import scala.concurrent.Future

object CourseDAO extends DAO(clazz = classOf[Course], collName="course", converter = CourseB.read) {

  /**
   * Saves the user's details
   */
  def saveDetails(c:Course):Future[Course] = coll.findOneAndReplace(
    equal("_id", IdB.write(c.id)),
    CourseB.write(c),
    FindOneAndReplaceOptions().upsert(true)
  ).head().flatMap(s => Future.fromTry(CourseB.read(s)))


  def saveSafe(c:Course) = {
    findAndReplace("_id" $eq c.id, CourseB.write(c), upsert=true).toRef
  }

  def saveNew(c:Course) = saveSafe(c).map(_ => c)

}
