package com.assessory.asyncmongo.converters

import com.assessory.api.critique._
import com.wbillingsley.handy.Id
import com.assessory.api._
import org.mongodb.scala.bson._

import scala.collection.JavaConverters._


import scala.util.{Failure, Try}

object CritAllocationB {

  def write(i: CritAllocation) = Document(
    "_id" -> IdB.write(i.id),
    "task" -> IdB.write(i.task),
    "completeBy" -> TargetB.write(i.completeBy),
    "allocation" -> i.allocation.map(AllocatedCritB.write)
  )

  def read(doc: Document): Try[CritAllocation] = Try {
    new CritAllocation(
      id = doc[BsonObjectId]("_id"),
      task = doc[BsonObjectId]("task"),
      completeBy = TargetB.read(Document(doc[BsonDocument]("completeBy"))).get,
      allocation = doc[BsonArray]("allocation").getValues.asScala.map({ case x => AllocatedCritB.read(Document(x.asDocument())).get })
    )
  }
}

object AllocatedCritB {


  def write(i: AllocatedCrit) = Document(
    "target" -> TargetB.write(i.target),
    "critique" -> IdB.write(i.critique)
  )

  def read(doc: Document): Try[AllocatedCrit] = Try {
    new AllocatedCrit(
      target = TargetB.read(Document(doc[BsonDocument]("target"))).get,
      critique = IdB.read(doc.get[BsonObjectId]("critique"))
    )
  }
}
