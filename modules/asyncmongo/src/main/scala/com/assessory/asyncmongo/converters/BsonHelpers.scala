package com.assessory.asyncmongo.converters

import com.wbillingsley.handy.{Ids, Id}
import org.mongodb.scala.bson._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters

import scala.util.Try

import scala.language.implicitConversions


/*
given Conversion[BsonString, String] = _.getValue

given Conversion[Option[BsonString], Option[String]] = _.map(_.getValue)

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

*/

object BsonHelpers {

  trait ToBson[T] {
    def toBson(i:T):BsonValue
  }

  trait FromBson[T] {
    def fromBson(b:BsonValue):T

    def tryFromBson(b:BsonValue) = Try { fromBson(b) }
  }

  trait ToFromBson[T] extends ToBson[T] with FromBson[T]

  implicit def docToBson(d:Document):BsonValue = d.toBsonDocument

  implicit def idToBson[T](id:Id[T,String]):BsonValue = IdB.write(id)

  implicit def strToBson(s:String):BsonValue = BsonString(s)

  implicit def ostrToBson(s:Option[String]):BsonValue = s match {
    case Some(s) => s
    case _ => BsonNull()
  }


  implicit def iToBson(s:Int):BsonValue = BsonInt32(s)

  implicit def lToBson(s:Long):BsonValue = BsonInt64(s)


  implicit def idsToBson[T](ids:Ids[T,String]):BsonArray = BsonArray.fromIterable(IdB.write(ids))

  implicit def seqIdToBson[T](ids:Seq[Id[T,String]]):BsonArray = BsonArray.fromIterable(ids.map(IdB.write(_)))

  implicit def toBsonSeq[T](items:Seq[T])(implicit tb:ToBson[T]):BsonArray = {
    val a = items.map(tb.toBson)
    BsonArray.fromIterable(a)
  }

  /*
   * DSL
   */

  def $set(tuples:(String,BsonValue)*) = Document("$set" -> Document.fromSeq(tuples))

  def $pull(tuples:(String,BsonValue)*) = Document("$pull" -> Document.fromSeq(tuples))

  def $push(tuples:(String,BsonValue)*) = Document("$push" -> Document.fromSeq(tuples))

  def $addToSet(tuples:(String,BsonValue)*) = Document("$addToSet" -> Document.fromSeq(tuples))

  def $in(arr:BsonArray) = Document("$in" -> arr).toBsonDocument

  implicit class keyOps(val s:String) extends AnyVal {
    def $eq(bson:BsonValue) = Filters.eq(s, bson)
  }

  implicit class bsonOps(val b:Bson) extends AnyVal {
    def and(bson:Bson) = Filters.and(b, bson)

    def or(bson:Bson) = Filters.or(b, bson)
  }

  def and(b:Bson*) = Filters.and(b:_*)

  def or(b:Bson*) = Filters.and(b:_*)

  val $null = new BsonNull

  def bsonDoc(tuples:(String, BsonValue)*) = Document.fromSeq(tuples).toBsonDocument

}
