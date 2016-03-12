package com.assessory.asyncmongo

import com.mongodb.WriteConcern
import com.mongodb.client.model._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.bson.{BsonArray, BsonObjectId}
import org.mongodb.scala.Observable
import org.mongodb.scala.bson._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{FindOneAndReplaceOptions, UpdateOptions}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import DB.executionContext

/**
  *
  * @param clazz A reference to the class object for the type this retrieves.
  * @param collName The name of the collection in the database
  * @param converter Converts from BSON to the transfer object
  * @tparam DataT The type this DAO retrieves
  */
class DAO[DataT <: HasStringId[DataT]] (
                                         clazz:Class[DataT],
                                         collName:String,
                                         converter: (Document => Try[DataT]),
                                         writeConcern: WriteConcern = WriteConcern.JOURNALED
                                       ) {

  implicit val executionContext = DB.executionContext

  implicit object LookUp extends LookUp[DataT, String] {

    def one[K <: String](r: Id[DataT, K]) = byId(r.id)

    def many[K <: String](r: Ids[DataT, K]) = manyById(r.ids)

  }

  def lookUp = LookUp

  /**
    * The collection in the database
    */
  def coll = DB.db.getCollection(collName).withWriteConcern(writeConcern)

  /**
    * Allocates a new ID
    */
  def allocateId = new ObjectId().toHexString

  def obsToRefMany(o:Observable[Document]):RefMany[DataT] = {
    for {
      seq <- o.toFuture().toRef
      doc <- seq.toRefMany
      converted <- converter(doc).toRef
    } yield converted
  }

  def findMany(query:Bson):RefMany[DataT] = {
    obsToRefMany(coll.find(query))
  }

  def findSorted(query:Bson, sort: Bson) = {
    obsToRefMany(coll.find(query).sort(sort))
  }

  def fFindOne(query:Bson):Future[DataT] = {
    for {
      doc <- coll.find(query).head()
      converted <- Future.fromTry(converter(doc))
    } yield converted
  }

  def findOne(query:Bson) = new RefFuture(fFindOne(query))


  /**
    * A test on whether _id matches
    */
  def idIs(id:Id[_,String]) = Filters.eq("_id", new ObjectId(id.id))

  /**
    * A test on whether _id is in the set.
    */
  def idsIn(ids:Ids[_, String]) = {
    val arr = for {
      id <- ids.ids
    } yield new BsonObjectId(new ObjectId(id))
    Filters.in("_id", new BsonArray(arr.asJava))
  }

  /**
    * Fetches and deserializes items by their ID
    */
  def byId(id:String) = {
    findOne(idIs(Id(id)))
  }

  /**
    * Fetches and deserializes items by their ID
    */
  def manyById(ids:Seq[String]):RefMany[DataT] = {
    /*
     * First, fetch the items.  These might not return in the same order as the
     * sequence of IDs, and duplicate IDs will only return once
     */
    val futureSeq = coll.find(idsIn(Ids(ids))).toFuture()

    val futureItems:Future[Seq[DataT]] = {
      futureSeq.map { docSeq =>
        docSeq.map { d => converter(d).get }
      }
    }

    /*
     * As the order of the items is unspecified, build a map from id -> item
     */
    val futIdMap = for {
      seq <- futureItems
    } yield {
      val pairs = for (item <- seq) yield item.id.id -> item
      val map = Map(pairs.toSeq:_*)
      map
    }

    /*
     * For each id in the requested sequence, return the corresponding item in the map.
     */
    val reordered = for (
      map <- new RefFuture(futIdMap);
      id <- new RefTraversableOnce(ids);
      item <- Ref(map.get(id))
    ) yield item

    reordered
  }

  def updateOneSafe(query:Bson, update:Bson, upsert:Boolean = false) = {
    coll.updateOne(
      query,
      update,
      UpdateOptions().upsert(upsert)
    ).head()
  }

  def updateManySafe(query:Bson, update:Bson, upsert:Boolean = false) = {
    coll.updateMany(query, update, UpdateOptions().upsert(upsert)).head()
  }

  def updateAndFetch(query:Bson, update:Bson, upsert:Boolean = false):Ref[DataT] = {
    for {
      ur <- new RefFuture(updateOneSafe(query, update, upsert))
      fetched <- findOne(query)
    } yield fetched
  }

  def updateSafe(query:Bson, update:Bson, item:DataT, upsert:Boolean = false):Ref[DataT] = {
    for {
      ur <- new RefFuture(updateOneSafe(query, update, upsert))
    } yield item
  }

  def findAndReplace(query:Bson, item:Document, upsert:Boolean=false) = {
    for {
      doc <- coll.findOneAndReplace(
        query,
        item,
        FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(upsert)
      ).head
      conv <- Future.fromTry(converter(doc))
    } yield conv
  }
}
