package com.assessory.api.question

import com.assessory.api.{TaskOutputBody, TaskOutput, TaskBody}
import com.wbillingsley.handy.{Id, HasStringId}

sealed trait Question extends HasStringId[Question]

sealed trait Answer {

  val question: Id[Question,String]

}

case class KindedQuestion[T <: Question](kind:String, q:T)
case class KindedAnswer[T <: Answer](kind:String, ans:T)

case class ShortTextQuestion(

  id:Id[Question,String],

  prompt: String,

  maxLength: Option[Int] = None

) extends Question


case class ShortTextAnswer(
  question: Id[Question, String],

  var answer: Option[String]
) extends Answer



case class BooleanQuestion(

  id:Id[Question,String],

  prompt: String

) extends Question


case class BooleanAnswer(
  question: Id[Question, String],

  var answer: Option[Boolean]
) extends Answer


case class QuestionnaireTask(
  questionnaire: Seq[Question]
) extends TaskBody {
  val kind = "Questionnaire"
}

case class QuestionnaireTaskOutput(
  answers: Seq[Answer]
) extends TaskOutputBody {
  val kind = "Questionnaire"
}