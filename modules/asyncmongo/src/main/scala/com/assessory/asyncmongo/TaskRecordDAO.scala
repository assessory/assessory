package com.assessory.asyncmongo

import com.assessory.api.{given, _}
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.{TargetB, TaskOutputBodyB, TaskOutputB, ByB, TaskRecordB, IdB}
import com.wbillingsley.handy.{Ref, RefMany, refOps, Id}
import refOps._
import org.mongodb.scala.WriteConcern
import com.wbillingsley.handy.RefOpt
import org.mongodb.scala.model.Updates.addEachToSet


object TaskRecordDAO extends DAO(classOf[TaskRecord], "taskRecord", TaskRecordB.read) with com.assessory.datalayer.TaskRecordDAO {

  import DB.given

  private def saveSafe(c:TaskRecord) = {
    findAndReplace("_id" $eq c.id, TaskRecordB.write(c), upsert=true).toRef
  }

  /**
    * The collection in the database
    */

  override def byTaskAndBy(task: TaskId, by: By, upsert: Boolean): RefOpt[TaskRecord] = ???

  override def pushTaskOutput(task: TaskId, by: By, outputId: TaskOutputId): Ref[TaskRecord] = {
    updateAndFetch(
      query = ("task" $eq task) and ("by" $eq ByB.write(by)),
      update = addEachToSet("outputs", IdB.write(outputId))
    ).orElse(saveSafe(
      TaskRecord(
        id = TaskRecordId(allocateId),
        task = task,
        by = by,
        outputs = Seq(outputId)
      )
    ).toRefOpt).require
  }

  

}