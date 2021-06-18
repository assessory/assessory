package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.{Ids, Id}
import Id._
import org.mongodb.scala.bson._
import Ids._

import scala.collection.JavaConverters._
import com.assessory.api.TaskId

object IdB {

  def write[T](i:Id[T, String]):BsonObjectId = BsonObjectId(i.id)

  def write[T](i:Option[Id[T, String]]):Option[BsonObjectId] = i.map(write(_))

  def write[T](i:Ids[T, String]):Seq[BsonObjectId] = i.ids.map(BsonObjectId.apply)

}
