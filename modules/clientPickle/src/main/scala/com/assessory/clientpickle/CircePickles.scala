package com.assessory.clientpickle

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.parser.decode

import scala.util.Try
import com.assessory.api.critique._
import com.assessory.api._
import com.assessory.api.client.{EmailAndPassword, WithPerms}
import com.assessory.api.due.{Due, DueDate, DuePerGroup, NoDue}
import com.assessory.api.video._
import question._
import com.wbillingsley.handy.appbase.{Question => _, Answer => _, _}
import com.wbillingsley.handy.{EmptyKind, Id, Ids, Ref}
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._

import scala.concurrent.Future

object Pickles {

  implicit def stringIdDecoder[T]: Decoder[Id[T, String]] = (c: HCursor) => {
    for {
      foo <- c.downField("id").as[String]
    } yield {
      foo.asId[T]
    }
  }

  implicit def stringIdEncoder[T]: Encoder[Id[T, String]] = (id: Id[T, String]) => Json.obj(
    "id" -> Json.fromString(id.id)
  )

  implicit def idKeyEncoder[T]: KeyEncoder[Id[T, String]] = (id: Id[T, String]) => id.id
  implicit def idKeyDecoder[T]: KeyDecoder[Id[T, String]] = (id: String) => Some(id.asId[T])

  implicit def stringIdsDecoder[T]: Decoder[Ids[T, String]] = deriveDecoder[Ids[T, String]]
  implicit def stringIdsEncoder[T]: Encoder[Ids[T, String]] = deriveEncoder[Ids[T, String]]

  implicit val questionEncoder: Encoder[Question] = deriveEncoder[Question]
  implicit val questionDecoder: Decoder[Question] = deriveDecoder[Question]

  implicit val courseRoleEncoder: Encoder[CourseRole] = deriveEncoder[CourseRole]
  implicit val courseRoleDecoder: Decoder[CourseRole] = deriveDecoder[CourseRole]

  implicit val ltiConsumerEncoder: Encoder[LTIConsumer] = deriveEncoder[LTIConsumer]
  implicit val ltiConsumerDecoder: Decoder[LTIConsumer] = deriveDecoder[LTIConsumer]
  implicit val courseEncoder: Encoder[Course] = deriveEncoder[Course]
  implicit val courseDecoder: Decoder[Course] = deriveDecoder[Course]

  implicit val identityEncoder: Encoder[Identity] = deriveEncoder[Identity]
  implicit val identityDecoder: Decoder[Identity] = deriveDecoder[Identity]

  implicit val courseRegEncoder: Encoder[Course.Reg] = (r:Course.Reg) => Json.obj(
    "id" -> r.id.asJson,
    "user" -> r.user.asJson,
    "target" -> r.target.asJson,
    "roles" -> r.roles.asJson,
    "updated" -> r.updated.asJson,
    "created" -> r.created.asJson
  )

  implicit val courseRegDecoder: Decoder[Course.Reg] = (c: HCursor) => {
    for {
      id <- c.downField("id").as[Id[Course.Reg, String]]
      user <- c.downField("user").as[Id[User, String]]
      target <- c.downField("target").as[Id[Course,String]]
      roles <- c.downField("roles").as[Set[CourseRole]]
      updated <- c.downField("updated").as[Long]
      created <- c.downField("created").as[Long]
    } yield {
      new Course.Reg(id=id, user=user, target=target, roles=roles, updated=updated, created=created, provenance=EmptyKind)
    }
  }

  implicit val identityLookupEncoder: Encoder[IdentityLookup] = deriveEncoder[IdentityLookup]
  implicit val identityLookupDecoder: Decoder[IdentityLookup] = deriveDecoder[IdentityLookup]

  implicit def usedEncoder[T]: Encoder[Used[T]] = (u:Used[T]) => Json.obj(
    "target" -> u.target.asJson, "time" -> u.time.asJson
  )

  implicit def usedDecoder[T]: Decoder[Used[T]] = (c: HCursor) => {
    for {
      target <- c.downField("target").as[Id[T, String]]
      time <- c.downField("time").as[Long]
    } yield Used(target, time)
  }

  implicit val coursePreenrolRowEncoder: Encoder[Course.PreenrolRow] = (r:Course.PreenrolRow) => Json.obj(
    "target" -> r.target.asJson,
    "identity" -> r.identity.asJson,
    "roles" -> r.roles.asJson,
    "used" -> r.used.asJson
  )

  implicit val coursePreenrolRowDecoder: Decoder[Course.PreenrolRow] = deriveDecoder[Course.PreenrolRow]

  implicit val coursePreenrolEncoder: Encoder[Course.Preenrol] = deriveEncoder[Course.Preenrol]
  implicit val coursePreenrolDecoder: Decoder[Course.Preenrol] = deriveDecoder[Course.Preenrol]

  implicit val groupSetEncoder: Encoder[GroupSet] = deriveEncoder[GroupSet]
  implicit val groupSetDecoder: Decoder[GroupSet] = deriveDecoder[GroupSet]

  implicit val groupEncoder: Encoder[Group] = deriveEncoder[Group]
  implicit val groupDecoder: Decoder[Group] = deriveDecoder[Group]


  implicit val questionnaireTaskEncoder: Encoder[QuestionnaireTask] = deriveEncoder[QuestionnaireTask]
  implicit val questionnaireTaskDecoder: Decoder[QuestionnaireTask] = deriveDecoder[QuestionnaireTask]

  implicit val targetTypeEncoder: Encoder[TargetType] = deriveEncoder[TargetType]
  implicit val targetTypeStrategyDecoder: Decoder[TargetType] = deriveDecoder[TargetType]
  implicit val critiqueTargetStrategyEncoder: Encoder[CritTargetStrategy] = deriveEncoder[CritTargetStrategy]
  implicit val critiqueTargetStrategyDecoder: Decoder[CritTargetStrategy] = deriveDecoder[CritTargetStrategy]
  implicit val critiqueTaskEncoder: Encoder[CritiqueTask] = deriveEncoder[CritiqueTask]
  implicit val critiqueTaskDecoder: Decoder[CritiqueTask] = deriveDecoder[CritiqueTask]

  implicit val dueEncoder: Encoder[Due] = deriveEncoder[Due]
  implicit val dueDecoder: Decoder[Due] = deriveDecoder[Due]
  implicit val taskRulesEncoder: Encoder[TaskRule] = deriveEncoder[TaskRule]
  implicit val taskRulesDecoder: Decoder[TaskRule] = deriveDecoder[TaskRule]
  implicit val taskDetailsEncoder: Encoder[TaskDetails] = deriveEncoder[TaskDetails]
  implicit val taskDetailsDecoder: Decoder[TaskDetails] = deriveDecoder[TaskDetails]

  implicit val taskBodyEncoder: Encoder[TaskBody] = (tb:TaskBody) => tb match {
    case qt:QuestionnaireTask => Json.obj("kind" -> Json.fromString("questionnaire"), "taskBody" -> qt.asJson)
    case ct:CritiqueTask => Json.obj("kind" -> Json.fromString("critique"), "taskBody" -> ct.asJson)
    case EmptyTaskBody => Json.obj("kind" -> Json.fromString("empty"))
  }

  implicit val taskBodyDecoder: Decoder[TaskBody] = (c: HCursor) => {
    c.downField("kind").as[String].flatMap {
      case "questionnaire" => c.downField("taskBody").as[QuestionnaireTask]
      case "critique" => c.downField("taskBody").as[CritiqueTask]
      case "empty" => Right(EmptyTaskBody)
    }
  }

  implicit val taskEncoder: Encoder[Task] = (t:Task) => Json.obj(
    "id" -> t.id.asJson,
    "course" -> t.course.asJson,
    "details" -> t.details.asJson,
    "body" -> t.body.asJson
  )
  implicit val taskDecoder: Decoder[Task] = deriveDecoder[Task]

  implicit val targetEncoder: Encoder[Target] = deriveEncoder[Target]
  implicit val targetDecoder: Decoder[Target] = deriveDecoder[Target]

  implicit val critiqueEncoder: Encoder[Critique] = deriveEncoder[Critique]
  implicit val critiqueDecoder: Decoder[Critique] = deriveDecoder[Critique]

  implicit val videoResourceEncoder: Encoder[VideoResource] = deriveEncoder[VideoResource]
  implicit val videoResourceDecoder: Decoder[VideoResource] = deriveDecoder[VideoResource]
  implicit val answerEncoder: Encoder[Answer] = deriveEncoder[Answer]
  implicit val answerDecoder: Decoder[Answer] = deriveDecoder[Answer]
  implicit val questionnaireTaskOutputEncoder: Encoder[QuestionnaireTaskOutput] = deriveEncoder[QuestionnaireTaskOutput]
  implicit val questionnaireTaskOutputDecoder: Decoder[QuestionnaireTaskOutput] = deriveDecoder[QuestionnaireTaskOutput]


  implicit val taskOutputBodyEncoder: Encoder[TaskOutputBody] = (tb:TaskOutputBody) => tb match {
    case qt:QuestionnaireTaskOutput => Json.obj("kind" -> Json.fromString("questionnaire"), "taskBody" -> qt.asJson)
    case ct:Critique => Json.obj("kind" -> Json.fromString("critique"), "taskBody" -> ct.asJson)
    case EmptyTaskOutputBody => Json.obj("kind" -> Json.fromString("empty"))
  }
  implicit val taskOutputBodyDecoder: Decoder[TaskOutputBody] = (c: HCursor) => {
    c.downField("kind").as[String].flatMap {
      case "questionnaire" => c.downField("taskBody").as[QuestionnaireTaskOutput]
      case "critique" => c.downField("taskBody").as[Critique]
      case "empty" => Right(EmptyTaskOutputBody)
    }
  }

  implicit val taskOutputDecoder: Decoder[TaskOutput] = deriveDecoder[TaskOutput]
  implicit val taskOutputEncoder: Encoder[TaskOutput] = deriveEncoder[TaskOutput]

  implicit val emailAndPasswordEncoder: Encoder[EmailAndPassword] = deriveEncoder[EmailAndPassword]
  implicit val emailAndPasswordDecoder: Decoder[EmailAndPassword] = deriveDecoder[EmailAndPassword]
  implicit val passwordLoginEncoder: Encoder[PasswordLogin] = deriveEncoder[PasswordLogin]
  implicit val PasswordLoginDecoder: Decoder[PasswordLogin] = deriveDecoder[PasswordLogin]
  implicit val activeSessionEncoder: Encoder[ActiveSession] = deriveEncoder[ActiveSession]
  implicit val activeSessionDecoder: Decoder[ActiveSession] = deriveDecoder[ActiveSession]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit def withPermsEncoder[T](implicit encoder: Encoder[T]): Encoder[WithPerms[T]] = deriveEncoder[WithPerms[T]]
  implicit def withPermsDecoder[T](implicit decoder: Decoder[T]): Decoder[WithPerms[T]] = deriveDecoder[WithPerms[T]]


  implicit val smallFileDetailsEncoder: Encoder[SmallFileDetails] = deriveEncoder[SmallFileDetails]
  implicit val smallFileDetailsDecoder: Decoder[SmallFileDetails] = deriveDecoder[SmallFileDetails]



  def write[T](thing: T)(implicit encoder: Encoder[T]): String = thing.asJson.toString

  def read[T](text: String)(implicit decoder: Decoder[T]): Try[T] = decode(text)(decoder).toTry

  def readF[T](text:String)(implicit decoder: Decoder[T]): Future[T] = Future.fromTry(read(text)(decoder))

  def readR[T](text:String)(implicit decoder: Decoder[T]): Ref[T] = Ref(read(text)(decoder))


}

