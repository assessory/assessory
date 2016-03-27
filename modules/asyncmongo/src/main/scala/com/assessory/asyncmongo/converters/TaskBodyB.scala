package com.assessory.asyncmongo.converters


import com.assessory.api.question.QuestionnaireTask
import com.assessory.api.video.VideoTask
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{GroupSet}
import com.assessory.api._
import critique._
import question._
import org.mongodb.scala.bson._
import scala.collection.JavaConverters._


import scala.util.{Failure, Try}

object TaskBodyB {
  def write(i: TaskBody):Document = i match {
    case c:CritiqueTask => CritiqueTaskB.write(c)
    case q:QuestionnaireTask => QuestionnaireTaskB.write(q)
    case v:VideoTask => VideoTaskB.write(v)
  }

  def read(doc: Document): Try[TaskBody] = {
    doc[BsonString]("kind").getValue match {
      case CritiqueTask.kind => CritiqueTaskB.read(doc)
      case "Questionnaire" => QuestionnaireTaskB.read(doc)
      case "Video" => VideoTaskB.read(doc)
      case k => Failure(new IllegalStateException("Couldn't parse task body with kind " + k))
    }
  }
}

object QuestionnaireTaskB  {

  def write(i: QuestionnaireTask) = Document(
    "kind" -> "Questionnaire",
    "questionnaire" -> i.questionnaire.map(QuestionB.write)
  )

  def read(doc: Document): Try[QuestionnaireTask] = Try {
    QuestionnaireTask(
      questionnaire = doc[BsonArray]("questionnaire").getValues.asScala.map({ case x => QuestionB.read(Document(x.asDocument())).get }).toSeq
    )
  }
}

object VideoTaskB  {

  def write(i: VideoTask) = Document(
    "kind" -> "Video"
  )

  def read(doc: Document): Try[VideoTask] = Try {
    VideoTask(

    )
  }
}

object CritiqueTaskB  {

  def write(i: CritiqueTask) = Document(
    "kind" -> "Critique",
    "strategy" -> CritTargetStrategyB.write(i.strategy),
    "task" -> TaskBodyB.write(i.task)
  )

  def read(doc: Document): Try[CritiqueTask] = Try {
    CritiqueTask(
      strategy = CritTargetStrategyB.read(Document(doc[BsonDocument]("strategy"))).get,
      task = TaskBodyB.read(Document(doc[BsonDocument]("task"))).get
    )
  }
}


object CritTargetStrategyB  {
  def write(i: CritTargetStrategy) = i match {
    case MyOutputStrategy(task) => Document("kind" -> "My output", "task" -> IdB.write(task))
    case OfMyGroupsStrategy(task) => Document("kind" -> "My group", "task" -> IdB.write(task))
    case PreallocateGroupStrategy(set, number) => Document("kind" -> "Preallocated groups", "set" -> IdB.write(set), "number" -> number)
  }

  def read(doc: Document): Try[CritTargetStrategy] = Try {
    doc[BsonString]("kind").getValue match {
      case "My output" => MyOutputStrategy(task = doc[BsonObjectId]("task"))
      case "My group" => OfMyGroupsStrategy(task = doc[BsonObjectId]("task"))
      case "Preallocated groups" => PreallocateGroupStrategy(
        set = doc[BsonObjectId]("set"),
        number = doc[BsonInt32]("number")
      )
    }
  }
}
