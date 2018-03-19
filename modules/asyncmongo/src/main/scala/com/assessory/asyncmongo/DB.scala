package com.assessory.asyncmongo

import com.wbillingsley.handy.Id
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.MongoClient
import Id._

import scala.concurrent.ExecutionContext

object DB {

  val mongoClient: MongoClient = MongoClient()

  var dbName = "assessory_2018_1"

  lazy val db = mongoClient.getDatabase(dbName)

  def allocateId:String = BsonObjectId.apply().getValue.toHexString

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

}
