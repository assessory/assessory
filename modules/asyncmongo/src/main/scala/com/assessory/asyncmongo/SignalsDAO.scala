package com.assessory.asyncmongo

import com.assessory.api._
import com.assessory.api.video.{SmallFileDetails, SmallFile}
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters._
import com.wbillingsley.handy.{Ref, refOps, Id}
import com.assessory.api.appbase.Course
import org.mongodb.scala.bson.collection.immutable.Document

import scala.concurrent.Future

object SignalsDAO extends DAO(classOf[Signal], "signal", SignalB.read) {

  import DB.given

  def saveSafe(f:Signal) = {
    findAndReplace("_id" $eq IdB.write(f.id), SignalB.write(f), upsert=true).toRef
  }


}