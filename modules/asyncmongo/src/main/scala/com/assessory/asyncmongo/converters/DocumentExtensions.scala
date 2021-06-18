package com.assessory.asyncmongo.converters

import com.wbillingsley.handy._
import org.mongodb.scala.bson._

/**
 * Extensions on the underlying (Java) BsonDocument
 */
extension (bd:BsonDocument) {

  def hexOid(key: String): String = bd.get(key).asObjectId.getValue.toHexString

}

/**
 * Extensions on the Scala Document class
 */
extension (d:Document) {

  def hexOid(key: String): String = d[BsonObjectId](key).getValue.toHexString
  def optHexOid(key: String): Option[String] = d.get[BsonObjectId](key).map(_.getValue.toHexString)

  def seqHexOid(key: String): Seq[String] = {
    import scala.collection.JavaConverters._
    var arr = d[BsonArray](key)
    arr.getValues.asScala.map(_.asObjectId().getValue.toHexString).toSeq
  }

  def boolean(key:String): Boolean = d[BsonBoolean](key).getValue
  def optBoolean(key:String): Option[Boolean] = d.get[BsonBoolean](key).map(_.getValue)

  def long(key:String): Long = d[BsonInt64](key).longValue
  def optLong(key:String): Option[Long] = d.get[BsonInt64](key).map(_.longValue)

  def int(key:String): Int = d[BsonInt32](key).intValue
  def optInt(key:String): Option[Int] = d.get[BsonInt32](key).map(_.intValue)

  def string(key:String): String = d[BsonString](key).getValue
  def optString(key:String): Option[String] = d.get[BsonString](key).map(_.getValue)

}

/**
 * Extensions on ids to allow them to write
 */
extension [T] (id:Id[T, String]) {

  def write:BsonValue = BsonObjectId(id.id)

}

extension [T] (seq:Seq[Id[T, String]]) {

  def write:Seq[BsonObjectId] = seq.map(i => BsonObjectId(i.id))

}

