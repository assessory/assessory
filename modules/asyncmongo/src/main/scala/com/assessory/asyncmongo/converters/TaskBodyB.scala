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

object TargetTypeB {
  def write(i:TargetType) = i match {
    case TTGroups(set) => Document("kind" -> "Groups", "set" -> IdB.write(set))
    case TTOutputs(task) => Document("kind" -> "Outputs", "task" -> IdB.write(task))
    case TTSelf => Document("kind" -> "Self")
  }

  def read(doc: Document): Try[TargetType] = Try {
    doc[BsonString]("kind").getValue match {
      case "Groups" => TTGroups(set = doc[BsonObjectId]("set"))
      case "Outputs" => TTOutputs(task = doc[BsonObjectId]("task"))
      case "Self" => TTSelf
    }

  }
}


object CritTargetStrategyB  {
  def write(i: CritTargetStrategy) = i match {
    case TargetMyStrategy(task, what, number) => Document(
      "kind" -> "Target My", "task" -> IdB.write(task), "what" -> TargetTypeB.write(what), "number" -> number
    )
    case AllocateStrategy(what, number) => Document(
      "kind" -> "Allocate", "what" -> TargetTypeB.write(what), "number" -> number
    )
    case AnyStrategy(what, number) => Document(
      "kind" -> "Any", "what" -> TargetTypeB.write(what), "number" -> number
    )
  }

  def read(doc: Document): Try[CritTargetStrategy] = {
    doc[BsonString]("kind").getValue match {
      case "Target My" => for {
        w <- TargetTypeB.read(Document(doc[BsonDocument]("what")))
      } yield TargetMyStrategy(
        task = doc[BsonObjectId]("task"),
        what = w,
        number = doc[BsonInt32]("number")
      )
      case "Allocate" => for {
        w <- TargetTypeB.read(Document(doc[BsonDocument]("what")))
      } yield AllocateStrategy(
        what = w,
        number = doc[BsonInt32]("number")
      )
      case "Any" => for {
        w <- TargetTypeB.read(Document(doc[BsonDocument]("what")))
      } yield AllocateStrategy(
        what = w,
        number = doc[BsonInt32]("number")
      )
    }
  }
}
