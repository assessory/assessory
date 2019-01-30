package com.assessory.clientpickle

import com.assessory.api.critique._
import com.assessory.api._
import com.assessory.api.video._
import question._
import com.wbillingsley.handy.appbase.{Used, IdentityLookup, CourseRole, Course}
import com.wbillingsley.handy.{Ids, Id}
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
//import upickle.Js


object UPickles {
/*
  def idWriter[T] = Pickles.Writer[Id[T, String]] { case id => Js.Str(id.id) }
  def idReader[T] = Pickles.Reader[Id[T, String]] { case Js.Str(s) => s.toString.asId[T] }


  def idsWriter[T] = Pickles.Writer[Ids[T, String]] { case id =>
    val jsIds = id.ids.map(Js.Str)
    Js.Arr(jsIds:_*)
  }
  def idsReader[T] = Pickles.Reader[Ids[T, String]] { case a:Js.Arr =>
    (for { v <- a.value } yield v.value.asInstanceOf[String]).asIds[T]
  }

  implicit def kqWriter[T](kq:KindedQuestion[T])(implicit qwriter:Pickles.Writer[T]) = Pickles.Writer {
    Js.Obj("kind" -> kq.kind, "q" -> kq.q)
  }

  val questionWriter:Pickles.Writer[Question] = Pickles.Writer {
    case st:ShortTextQuestion => Pickles.writeJs(KindedQuestion("Short Text", st))
    case q:BooleanQuestion => Pickles.writeJs(KindedQuestion("Boolean", q))
    case q:VideoQuestion => Pickles.writeJs(KindedQuestion("Video", q))
    case q:FileQuestion => Pickles.writeJs(KindedQuestion("SmallFile", q))
  }
  val questionReader:Pickles.Reader[Question] = Pickles.Reader {
    case o:Js.Obj => o("kind") match {
      case Js.Str("Short Text") => Pickles.readJs[KindedQuestion[ShortTextQuestion]](o).q
      case Js.Str("Boolean") => Pickles.readJs[KindedQuestion[BooleanQuestion]](o).q
      case Js.Str("Video") => Pickles.readJs[KindedQuestion[VideoQuestion]](o).q
      case Js.Str("SmallFile") => Pickles.readJs[KindedQuestion[FileQuestion]](o).q
    }
  }

  implicit val answerWriter:Pickles.Writer[Answer] = Pickles.Writer {
    case st:ShortTextAnswer => Pickles.writeJs(KindedAnswer("Short Text", st))
    case q:BooleanAnswer => Pickles.writeJs(KindedAnswer("Boolean", q))
    case q:VideoAnswer => Pickles.writeJs(KindedAnswer("Video", q))
    case q:FileAnswer => Pickles.writeJs(KindedAnswer("SmallFile", q))
  }
  implicit val answerReader:Pickles.Reader[Answer]  = Pickles.Reader {
    case o:Js.Obj => o("kind") match {
      case Js.Str("Short Text") => Pickles.readJs[KindedAnswer[ShortTextAnswer]](o).ans
      case Js.Str("Boolean") => Pickles.readJs[KindedAnswer[BooleanAnswer]](o).ans
      case Js.Str("Video") => Pickles.readJs[KindedAnswer[VideoAnswer]](o).ans
      case Js.Str("SmallFile") => Pickles.readJs[KindedAnswer[FileAnswer]](o).ans
    }
  }

  implicit val taskBodyWriter:Pickles.Writer[TaskBody] = Pickles.Writer[TaskBody] {
    case ct:CritiqueTask => Js.Obj(
      "kind" -> Js.Str(CritiqueTask.kind),
      "strategy" -> Pickles.writeJs(ct.strategy),
      "task" -> Pickles.writeJs(ct.task)(taskBodyWriter)
    )
    case q:QuestionnaireTask => Pickles.writeJs(KindedTaskBody(q.kind, q))
    case v:VideoTask => Pickles.writeJs(KindedTaskBody("Video", v))
    case EmptyTaskBody => Pickles.writeJs(KindedTaskBody("Empty", EmptyTaskBody))
    case m:MessageTask => Pickles.writeJs(KindedTaskBody("Message", m))
    case f:SmallFileTask => Pickles.writeJs(KindedTaskBody("SmallFile", f))
    case c:CompositeTask => Pickles.writeJs(KindedTaskBody("Composite", c))
  }
  implicit val taskBodyReader:Pickles.Reader[TaskBody] = Pickles.Reader[TaskBody] {
    case o:Js.Obj =>
      o("kind") match {
        case Js.Str(CritiqueTask.kind) => CritiqueTask(
          strategy = Pickles.readJs[CritTargetStrategy](o("strategy")),
          task = Pickles.readJs(o("task"))(taskBodyReader)
        )
        case Js.Str("Questionnaire") => Pickles.readJs[KindedTaskBody[QuestionnaireTask]](o).taskBody
        case Js.Str("Video") => Pickles.readJs[KindedTaskBody[VideoTask]](o).taskBody
        case Js.Str("Empty") => EmptyTaskBody
        case Js.Str("SmallFile") => Pickles.readJs[KindedTaskBody[SmallFileTask]](o).taskBody
        case Js.Str("Message") => Pickles.readJs[KindedTaskBody[MessageTask]](o).taskBody
        case Js.Str("Composite") => Pickles.readJs[KindedTaskBody[CompositeTask]](o).taskBody
      }
  }

  implicit val taskOutputBodyWriter:Pickles.Writer[TaskOutputBody] = Pickles.Writer {
    case ct:Critique => Js.Obj(
      "kind" -> Js.Str(CritiqueTask.kind),
      "target" -> Pickles.writeJs(ct.target),
      "task" -> Pickles.writeJs(ct.task)(taskOutputBodyWriter)
    )
    case EmptyTaskOutputBody => Pickles.writeJs(KindedTaskOutputBody(EmptyTaskOutputBody.kind, EmptyTaskOutputBody))
    case v:VideoTaskOutput => Js.Obj(
      "kind" -> Js.Str("video"), "video" -> Pickles.writeJs(v.video)
    )
    case q:QuestionnaireTaskOutput => Js.Obj(
      "kind" -> Js.Str("questionnaire"), "answers" -> Pickles.writeJs(q.answers)
    )
  }

  implicit val taskOutputBodyReader:Pickles.Reader[TaskOutputBody] = Pickles.Reader {
    case o:Js.Obj =>
      o("kind") match {
        case Js.Str(CritiqueTask.kind) => Critique(
          target = Pickles.readJs[Target](o("target")),
          task = Pickles.readJs(o("task"))(taskOutputBodyReader)
        )
        case Js.Str("video") => VideoTaskOutput(video = Pickles.readJs[Option[VideoResource]](o("video")))
        case Js.Str("questionnaire") => QuestionnaireTaskOutput(answers = Pickles.readJs[Seq[Answer]](o("answers")))
        case Js.Str(EmptyTaskOutputBody.kind) => EmptyTaskOutputBody
      }
  }

  implicit val coursePreenrolRowReader = Pickles.Reader[Course.PreenrolRow] { case o:Js.Obj =>
    new Course.PreenrolRow(
      target = Pickles.readJs[Id[Course,String]](o("target")),
      roles = Pickles.readJs[Set[CourseRole]](o("roles")),
      identity = Pickles.readJs[IdentityLookup](o("identity")),
      used = Pickles.readJs[Option[Used[Course.Reg]]](o("used"))
    )
  }

  implicit val coursePreenrolRowWriter = Pickles.Writer[Course.PreenrolRow] { case row =>
    Js.Obj(
      "target" -> Pickles.writeJs(row.target),
      "roles" -> Pickles.writeJs(row.roles),
      "identity" -> Pickles.writeJs(row.identity),
      "used" -> Pickles.writeJs(row.used)
    )
  }

  implicit val coursePreenrolReader = Pickles.Reader[Course.Preenrol] { case o:Js.Obj =>
    new Course.Preenrol(
      id = Pickles.readJs[Id[Course.Preenrol,String]](o("id")),
      name = Pickles.readJs[Option[String]](o("name")),
      within = Pickles.readJs[Option[Id[Course,String]]](o("within")),
      rows = Pickles.readJs[Seq[Course.PreenrolRow]](o("rows")),
      created = Pickles.readJs[Long](o("created")),
      modified = Pickles.readJs[Long](o("modified"))
    )
  }
  implicit val coursePreenrolWriter = Pickles.Writer[Course.Preenrol] { case p =>
    Js.Obj(
      "id" -> Pickles.writeJs(p.id),
      "name" -> Pickles.writeJs(p.name),
      "within" -> Pickles.writeJs(p.within),
      "rows" -> Pickles.writeJs(p.rows),
      "created" -> Pickles.writeJs(p.created),
      "modified" -> Pickles.writeJs(p.modified)
    )
  }


  implicit val allocatedCritReader = Pickles.Reader[AllocatedCrit] { case o:Js.Obj =>
    new AllocatedCrit(
      target = Pickles.readJs[Target](o("target")),
      critique = Pickles.readJs[Option[Id[TaskOutput,String]]](o("critique"))
    )
  }
  implicit val allocatedCritWriter = Pickles.Writer[AllocatedCrit] { case row =>
    Js.Obj(
      "target" -> Pickles.writeJs(row.target),
      "critique" -> Pickles.writeJs(row.critique)
    )
  }

*/

}
