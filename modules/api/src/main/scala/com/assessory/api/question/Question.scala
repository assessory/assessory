package com.assessory.api.question

import com.assessory.api.video.{SmallFile, VideoResource}
import com.assessory.api.{TaskOutputBody, TaskOutput, TaskBody}
import com.wbillingsley.handy.{Id, HasId}

sealed trait Question extends HasId[Id[Question, String]] {

  def prompt:String

  def hideInCrit:Boolean

  def blankAnswer:Answer

}

case class QuestionId(id:String) extends Id[Question, String]

sealed trait Answer {

  val question: Id[Question,String]

}

case class KindedQuestion[T <: Question](kind:String, q:T)
case class KindedAnswer[T <: Answer](kind:String, ans:T)

case class ShortTextQuestion(

  id:Id[Question,String],

  prompt: String,

  maxLength: Option[Int] = None,

  hideInCrit: Boolean = false

) extends Question {
  def blankAnswer = ShortTextAnswer(question=id, answer=None)
}




case class ShortTextAnswer(
  question: Id[Question, String],

  var answer: Option[String]

) extends Answer



case class BooleanQuestion(

  id:Id[Question,String],

  prompt: String,

  hideInCrit: Boolean = true

) extends Question {
  def blankAnswer = BooleanAnswer(question=id, answer=None)
}


case class BooleanAnswer(
  question: Id[Question, String],

  var answer: Option[Boolean]
) extends Answer

case class VideoQuestion(

  id:Id[Question,String],

  prompt: String,

  hideInCrit: Boolean = false

) extends Question {
  def blankAnswer = VideoAnswer(question=id, answer=None)
}

case class VideoAnswer(
  question: Id[Question, String],

  var answer: Option[VideoResource] = None
) extends Answer

case class FileQuestion(

  id:Id[Question,String],

  prompt: String,

  hideInCrit: Boolean = false

) extends Question {
  def blankAnswer = FileAnswer(question=id, answer=None)
}

case class FileAnswer(
  question: Id[Question, String],

  var answer: Option[Id[SmallFile, String]] = None
) extends Answer

case class QuestionnaireTask(
  questionnaire: Seq[Question]
) extends TaskBody {

  lazy val questionMap:Map[Id[Question,String], Question] = (for { q <- questionnaire } yield q.id -> q ).toMap

  val kind = "Questionnaire"
}

case class QuestionnaireTaskOutput(
  answers: Seq[Answer]
) extends TaskOutputBody {
  val kind = "Questionnaire"
}