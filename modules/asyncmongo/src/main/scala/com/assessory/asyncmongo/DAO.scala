package com.assessory.asyncmongo

import com.mongodb.WriteConcern
import com.mongodb.client.model._
import com.wbillingsley.handy.{HasId, Id, LookUp, Ref, RefFuture, RefMany, RefOpt, refOps}
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.bson.{BsonArray, BsonObjectId}
import org.mongodb.scala.{Observable, Observer, Subscription}
import org.mongodb.scala.bson._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{FindOneAndReplaceOptions, UpdateOptions}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import DAO._
import com.assessory.api.wiring.IdLookUp

/**
  *
  * @param clazz A reference to the class object for the type this retrieves.
  * @param collName The name of the collection in the database
  * @param converter Converts from BSON to the transfer object
  * @tparam DataT The type this DAO retrieves
  */
class DAO[DataT <: HasId[Id[DataT, String]]] (
                                         clazz:Class[DataT],
                                         collName:String,
                                         converter: (Document => Try[DataT]),
                                         writeConcern: WriteConcern = WriteConcern.JOURNALED
                                       ) {

  import DB.given

  implicit object LookUp extends IdLookUp[DataT] {

    override def eagerOne(r: Id[DataT, String]) = byId(r.id).require
    override def eagerOpt(r: Id[DataT, String]) = byId(r.id)
    override def many(seq: Seq[Id[DataT, String]]) = manyById(seq.map(_.id))

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

  /**
    * Is this string valid as an ID?
    */
  def validId(s:String) = try {
    new ObjectId(s).toHexString == s
  } catch {
    case ex:Exception => false
  }

  def obsToRefMany(o:Observable[Document]):RefMany[DataT] = {
    for {
      seq <- o.collect().head().toRef     // FIXME: Should use streaming instead
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

  def findOne(query:Bson):RefOpt[DataT] = {
    for {
      opt <- coll.find(query).headOption().toRef
      doc <- opt.toRefOpt
      converted <- Future.fromTry(converter(doc)).toRef
    } yield converted
  }

  /**
    * A test on whether _id matches
    */
  def idIs(id:Id[_,String]) = Filters.eq("_id", new ObjectId(id.id))

  /**
    * A test on whether _id is in the set.
    */
  def idsIn(ids:Seq[Id[_, String]]) = {
    val arr = for {
      id <- ids
    } yield new BsonObjectId(new ObjectId(id.id))
    Filters.in("_id", new BsonArray(arr.asJava))
  }

  def idsInStr(ids:Seq[String]) = {
    val arr = for {
      id <- ids
    } yield new BsonObjectId(new ObjectId(id))
    Filters.in("_id", new BsonArray(arr.asJava))
  }

  /**
    * Fetches and deserializes items by their ID
    */
  def byId(id:String) = {
    findOne(Filters.eq("_id", new ObjectId(id)))
  }

  /**
    * Fetches and deserializes items by their ID
    */
  def manyById(ids:Seq[String]):RefMany[DataT] = {
    /*
     * First, fetch the items.  These might not return in the same order as the
     * sequence of IDs, and duplicate IDs will only return once
     */
    val futureSeq = coll.find(idsInStr(ids)).collect().head() // FIXME: Should use streaming instead

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
      id <- ids.toRefMany;
      item <- RefOpt(map.get(id))
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

  def updateAndFetch(query:Bson, update:Bson, upsert:Boolean = false):RefOpt[DataT] = {
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

object DAO {

  implicit class ObservableWithHeadOption[T](val observable:Observable[T]) extends AnyVal {

    def headOption(): Future[Option[T]] = {
      val promise = Promise[Option[T]]()
      observable.subscribe(new Observer[T]() {
        @volatile
        var subscription: Option[Subscription] = None

        override def onError(throwable: Throwable): Unit = promise.failure(throwable)

        override def onSubscribe(sub: Subscription): Unit = {
          subscription = Some(sub)
          sub.request(1)
        }

        override def onComplete(): Unit = {
          subscription.get.unsubscribe()
          promise.success(None)
        }

        override def onNext(tResult: T): Unit = {
          subscription.get.unsubscribe()
          promise.success(Some(tResult))
        }
      })
      promise.future
    }

  }


}