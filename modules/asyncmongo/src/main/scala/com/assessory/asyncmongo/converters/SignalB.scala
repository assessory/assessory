package com.assessory.asyncmongo.converters


import com.assessory.api.question.QuestionnaireTask
import com.assessory.api.video.{CompositeTask, MessageTask, SmallFileTask, VideoTask}
import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{GroupSet, GroupSetId}
import com.assessory.api._
import critique._
import question._
import org.mongodb.scala.bson._

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}


object SignalBodyB {
  def write(i: SignalBody) = i match
    case CritiqueAllocated(task, taskOutput, critiqueTask, critique) => 
      Document("kind" -> "CritiqueAllocated", "task" -> IdB.write(task), "taskOutput" -> IdB.write(taskOutput), "critiqueTask" -> IdB.write(critiqueTask), "critique" -> IdB.write(critique))

  def read(doc: Document): Try[SignalBody] = {
    Try { doc[BsonString]("kind").getValue match {
      case "CritiqueAllocated" => 
        CritiqueAllocated(
          task = TaskId(doc.hexOid("task")),
          taskOutput = TaskOutputId(doc.hexOid("taskOutput")),
          critiqueTask = TaskId(doc.hexOid("critiqueTask")),
          critique = TaskOutputId(doc.hexOid("critique")),
        )
    }}
  }

}

object SignalB {
  def write(i: Signal) = Document(
        "_id" -> IdB.write(i.id),
        "task" -> IdB.write(i.task),
        "taskOutput" -> IdB.write(i.taskOutput),
        "created" -> i.created,
        "body" -> SignalBodyB.write(i.body)
  )

  def read(doc: Document): Try[Signal] = Try {
    new Signal(
      id = SignalId(doc.hexOid("_id")),
      task = TaskId(doc.hexOid("task")),
      taskOutput = TaskOutputId(doc.hexOid("taskOutput")),
      created = doc.long("created"),
      body = SignalBodyB.read(Document(doc[BsonDocument]("body"))).get
    )
  }
}
