package com.assessory.api.question

import com.wbillingsley.handy.{Id, HasStringId}

trait Question extends HasStringId[Question]

trait Answer[T] {

  val question: Id[Question,String]

  var answer: Option[T]

}


case class ShortTextQuestion(

  id:Id[Question,String],

  prompt: String,

  maxLength: Option[Int] = None

) extends Question


case class ShortTextAnswer(
  question: Id[Question, String],

  var answer: Option[String]
) extends Answer[String]



case class BooleanQuestion(

  id:Id[Question,String],

  prompt: String

) extends Question


case class BooleanAnswer(
  question: Id[Question, String],

  var answer: Option[Boolean]
) extends Answer[Boolean]

