package com.assessory.datalayer

import com.assessory.api.By
import com.assessory.api.TaskRecord
import com.assessory.api.TaskId
import com.wbillingsley.handy.{Ref, RefOpt}
import com.assessory.api.TaskOutputId

/**
  * TaskRecords don't have their own (externally addressible) ID. 
  * They are a record of what a user or group has done on a Task
  */
trait TaskRecordDAO {

    /** Looks up a TaskRecord, optionally creating a blank if there isn't one */
    def byTaskAndBy(task:TaskId, by:By, upsert:Boolean = true):RefOpt[TaskRecord] 

    /** Adds an output to a task record */
    def pushTaskOutput(task:TaskId, by:By, outputId:TaskOutputId):Ref[TaskRecord]

}
