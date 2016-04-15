package com.assessory.clientpickle

import com.assessory.api.critique._
import com.assessory.api._
import question._
import com.wbillingsley.handy.appbase.{Used, IdentityLookup, CourseRole, Course}
import com.wbillingsley.handy.{Ids, Id}
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import upickle.Js


object Pickles {

  def idWriter[T] = upickle.default.Writer[Id[T, String]] { case id => Js.Str(id.id) }
  def idReader[T] = upickle.default.Reader[Id[T, String]] { case Js.Str(s) => s.asId[T] }


  def idsWriter[T] = upickle.default.Writer[Ids[T, String]] { case id =>
    val jsIds = id.ids.map(Js.Str)
    Js.Arr(jsIds:_*)
  }
  def idsReader[T] = upickle.default.Reader[Ids[T, String]] { case a:Js.Arr =>
    (for { v <- a.value } yield v.value.asInstanceOf[String]).asIds[T]
  }


  val questionWriter:upickle.default.Writer[Question] = upickle.default.Writer {
    case st:ShortTextQuestion => upickle.default.writeJs(KindedQuestion("Short Text", st))
    case q:BooleanQuestion => upickle.default.writeJs(KindedQuestion("Boolean", q))
  }
  val questionReader:upickle.default.Reader[Question] = upickle.default.Reader {
    case o:Js.Obj => o("kind") match {
      case Js.Str("Short Text") => upickle.default.readJs[KindedQuestion[ShortTextQuestion]](o).q
      case Js.Str("Boolean") => upickle.default.readJs[KindedQuestion[BooleanQuestion]](o).q
    }
  }

  val answerWriter:upickle.default.Writer[Answer] = upickle.default.Writer {
    case st:ShortTextAnswer => upickle.default.writeJs(KindedAnswer("Short Text", st))
    case q:BooleanAnswer => upickle.default.writeJs(KindedAnswer("Boolean", q))
  }
  val answerReader:upickle.default.Reader[Answer]  = upickle.default.Reader {
    case o:Js.Obj => o("kind") match {
      case Js.Str("Short Text") => upickle.default.readJs[KindedAnswer[ShortTextAnswer]](o).ans
      case Js.Str("Boolean") => upickle.default.readJs[KindedAnswer[BooleanAnswer]](o).ans
    }
  }

  implicit val taskBodyWriter:upickle.default.Writer[TaskBody] = upickle.default.Writer[TaskBody] {
    case ct:CritiqueTask => Js.Obj(
      "kind" -> Js.Str(CritiqueTask.kind),
      "strategy" -> upickle.default.writeJs(ct.strategy),
      "task" -> upickle.default.writeJs(ct.task)(taskBodyWriter)
    )
    case q:QuestionnaireTask => upickle.default.writeJs(KindedTaskBody(q.kind, q))
    case EmptyTaskBody => upickle.default.writeJs(KindedTaskBody("Empty", EmptyTaskBody))
  }
  implicit val taskBodyReader:upickle.default.Reader[TaskBody] = upickle.default.Reader[TaskBody] {
    case o:Js.Obj =>
      o("kind") match {
        case Js.Str(CritiqueTask.kind) => CritiqueTask(
          strategy = upickle.default.readJs[CritTargetStrategy](o("strategy")),
          task = upickle.default.readJs(o("task"))(taskBodyReader)
        )
        case Js.Str("Questionnaire") => upickle.default.readJs[KindedTaskBody[QuestionnaireTask]](o).taskBody
        case Js.Str("Empty") => EmptyTaskBody
      }
  }

  implicit val taskOutputBodyWriter:upickle.default.Writer[TaskOutputBody] = upickle.default.Writer {
    case ct:Critique => Js.Obj(
      "kind" -> Js.Str(CritiqueTask.kind),
      "target" -> upickle.default.writeJs(ct.target),
      "task" -> upickle.default.writeJs(ct.task)(taskOutputBodyWriter)
    )
    case EmptyTaskOutputBody => upickle.default.writeJs(KindedTaskOutputBody(EmptyTaskOutputBody.kind, EmptyTaskOutputBody))
  }
  implicit val taskOutputBodyReader:upickle.default.Reader[TaskOutputBody] = upickle.default.Reader {
    case o:Js.Obj =>
      o("kind") match {
        case Js.Str(CritiqueTask.kind) => Critique(
          target = upickle.default.readJs[Target](o("target")),
          task = upickle.default.readJs(o("task"))(taskOutputBodyReader)
        )
        case Js.Str(EmptyTaskOutputBody.kind) => EmptyTaskOutputBody
      }
  }

  implicit val coursePreenrolRowReader = upickle.default.Reader[Course.PreenrolRow] { case o:Js.Obj =>
    new Course.PreenrolRow(
      target = upickle.default.readJs[Id[Course,String]](o("target")),
      roles = upickle.default.readJs[Set[CourseRole]](o("roles")),
      identity = upickle.default.readJs[IdentityLookup](o("identity")),
      used = upickle.default.readJs[Option[Used[Course.Reg]]](o("used"))
    )
  }
  implicit val coursePreenrolRowWriter = upickle.default.Writer[Course.PreenrolRow] { case row =>
    Js.Obj(
      "target" -> upickle.default.writeJs(row.target),
      "roles" -> upickle.default.writeJs(row.roles),
      "identity" -> upickle.default.writeJs(row.identity),
      "used" -> upickle.default.writeJs(row.used)
    )
  }

  implicit val coursePreenrolReader = upickle.default.Reader[Course.Preenrol] { case o:Js.Obj =>
    new Course.Preenrol(
      id = upickle.default.readJs[Id[Course.Preenrol,String]](o("id")),
      name = upickle.default.readJs[Option[String]](o("name")),
      within = upickle.default.readJs[Option[Id[Course,String]]](o("within")),
      rows = upickle.default.readJs[Seq[Course.PreenrolRow]](o("rows")),
      created = upickle.default.readJs[Long](o("created")),
      modified = upickle.default.readJs[Long](o("modified"))
    )
  }
  implicit val coursePreenrolWriter = upickle.default.Writer[Course.Preenrol] { case p =>
    Js.Obj(
      "id" -> upickle.default.writeJs(p.id),
      "name" -> upickle.default.writeJs(p.name),
      "within" -> upickle.default.writeJs(p.within),
      "rows" -> upickle.default.writeJs(p.rows),
      "created" -> upickle.default.writeJs(p.created),
      "modified" -> upickle.default.writeJs(p.modified)
    )
  }


  implicit val allocatedCritReader = upickle.default.Reader[AllocatedCrit] { case o:Js.Obj =>
    new AllocatedCrit(
      target = upickle.default.readJs[Target](o("target")),
      critique = upickle.default.readJs[Option[Id[TaskOutput,String]]](o("critique"))
    )
  }
  implicit val allocatedCritWriter = upickle.default.Writer[AllocatedCrit] { case row =>
    Js.Obj(
      "target" -> upickle.default.writeJs(row.target),
      "critique" -> upickle.default.writeJs(row.critique)
    )
  }



}
