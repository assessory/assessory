package com.assessory.asyncmongo

import com.wbillingsley.handy.{Ids, Id}
import org.mongodb.scala.bson._

import scala.language.implicitConversions

package object converters {

  implicit def bsonStrToStr(s:BsonString):String = s.getValue

  implicit def oBsonStrToStr(s:Option[BsonString]):Option[String] = s.map(_.getValue)

  implicit def bsonInt64ToL(s:BsonInt64):Long = s.getValue

  implicit def obsonInt64ToL(s:Option[BsonInt64]):Option[Long] = s.map(_.getValue)

  implicit def bsonInt32ToI(s:BsonInt32):Int = s.getValue

  implicit def obsonInt32ToI(s:Option[BsonInt32]):Option[Int] = s.map(_.getValue)

  implicit def bsonBoolToBool(s:BsonBoolean):Boolean = s.getValue

  implicit def obsonBoolToBool(s:Option[BsonBoolean]):Option[Boolean] = s.map(_.getValue)

  implicit def bsonObjIdToId[T](s:BsonObjectId):Id[T,String] = IdB.read(s)

  implicit def obsonObjIdToId[T](s:Option[BsonObjectId]):Option[Id[T,String]] = s.map(bsonObjIdToId)

  implicit def bsonArrToIds[T](s:BsonArray):Ids[T,String] = IdB.read(s)


  implicit def idTtoBsonObjectId[T](i:Id[T, String]):BsonObjectId = BsonObjectId(i.id)

}
