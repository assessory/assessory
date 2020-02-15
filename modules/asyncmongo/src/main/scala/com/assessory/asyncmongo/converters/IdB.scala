package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.{Ids, Id}
import Id._
import org.mongodb.scala.bson._
import Ids._

import scala.collection.JavaConverters._

object IdB {

  def write[T](i:Id[T, String]):BsonObjectId = BsonObjectId(i.id)

  def write[T](i:Option[Id[T, String]]):Option[BsonObjectId] = i.map(write(_))

  def read[T](s:BsonObjectId):Id[T,String] = s.getValue.toHexString.asId[T]

  def read[T](s:Option[BsonObjectId]):Option[Id[T,String]] = s.map(read(_))


  def write[T](i:Ids[T, String]):Seq[BsonObjectId] = i.ids.map(BsonObjectId.apply)

  def read[T](s:BsonArray):Ids[T,String] = {
    val it = s.getValues.asScala.map(_.asObjectId().getValue.toHexString).toSeq
    it.asIds[T]
  }

}
