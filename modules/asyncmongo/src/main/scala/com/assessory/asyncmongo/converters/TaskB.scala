package com.assessory.asyncmongo.converters

import com.assessory.api.due.Due
import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{Course, CourseId, Group, GroupSet, GroupSetId}
import com.assessory.api._
import org.mongodb.scala.bson._

import scala.collection.JavaConverters._
import scala.util.{Failure, Try}

object TaskB  {

  def write(i: Task) = Document(
    "_id" -> IdB.write(i.id),
    "course" -> IdB.write(i.course),
    "details" -> TaskDetailsB.write(i.details),
    "body" -> TaskBodyB.write(i.body)
  )

  def read(doc: Document): Try[Task] = Try {
    new Task(
      id = TaskId(doc[BsonObjectId]("_id").getValue.toHexString),
      course = CourseId(doc[BsonObjectId]("course").getValue.toHexString),
      details = TaskDetailsB.read(Document(doc[BsonDocument]("details"))).get,
      body = TaskBodyB.read(Document(doc[BsonDocument]("body"))).get
    )
  }
}

object TaskDetailsB {
  def write(i: TaskDetails) = Document(
    "name" -> i.name,
    "description" -> i.description,
    "groupSet" -> IdB.write(i.groupSet),
    "restrictions" -> i.restrictions.map(TaskRuleB.write),
    "individual" -> i.individual,
    "published" -> DueB.write(i.published),
    "open" -> DueB.write(i.open),
    "due" -> DueB.write(i.due),
    "closed" -> DueB.write(i.closed),
    "created" -> i.created
  )

  def read(doc: Document): Try[TaskDetails] = Try {
    new TaskDetails(
      name = doc.optString("name"),
      description = doc.optString("description"),
      groupSet = doc.optHexOid("groupSet").map(GroupSetId(_)),
      individual = doc.boolean("individual"),
      restrictions = doc.get[BsonArray]("restrictions").map(_.getValues.asScala.map({ case x => TaskRuleB.read(Document(x.asDocument())).get }).toSeq).getOrElse(Seq.empty),
      published = DueB.read(Document(doc[BsonDocument]("published"))).get,
      open = DueB.read(Document(doc[BsonDocument]("open"))).get,
      due = DueB.read(Document(doc[BsonDocument]("due"))).get,
      closed = DueB.read(Document(doc[BsonDocument]("closed"))).get,
      created = doc.long("created")
    )
  }
}

object TaskRuleB {

  def write(r:TaskRule) = r match {
    case MustHaveFinished(t) => Document("kind" -> "MustHaveFinished", "task" -> IdB.write(t))
  }

  def read(doc:Document):Try[TaskRule] = {
    doc[BsonString]("kind").getValue match {
      case "MustHaveFinished" => Try { MustHaveFinished(task = TaskId(doc.hexOid("task"))) }
    }
  }

}

