package com.assessory.asyncmongo

import com.assessory.api._
import com.assessory.api.video.{SmallFileDetails, SmallFile}
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters._
import com.wbillingsley.handy.{Ref, refOps, Id}
import com.assessory.api.appbase.Course
import org.mongodb.scala.bson.collection.immutable.Document

import scala.concurrent.Future

object SmallFileDAO extends DAO(classOf[SmallFile], "smallFile", SmallFileB.read) {

  import DB.given

  def saveSafe(f:SmallFile) = {
    findAndReplace("_id" $eq IdB.write(f.id), SmallFileB.write(f), upsert=true).toRef
  }

  def getDetails(i:Id[SmallFile, String]):Ref[SmallFileDetails] = {
    coll.find("_id" $eq IdB.write(i)).projection(Document("details" -> 1)).head.flatMap({ case d =>
      println("reading a doc"  + SmallFileB.read(d))
      Future.fromTry(SmallFileB.read(d).map(_.details))
    }).toRef
  }

}

