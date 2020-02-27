package com.assessory.clientpickle

import com.assessory.api.Task
import com.assessory.api.call._
import com.wbillingsley.handy.{Id, Ref}
import com.wbillingsley.handy.appbase.{ActiveSession, Course, Group, GroupSet, User}
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

import scala.concurrent.Future
import scala.util.Try

object CallPickles {

  import Pickles._

  val k = "kind"
  val b = "body"

  implicit val registerEncoder: Encoder[Register] = (r:Register) => Json.obj(
    "email" -> r.email.asJson, "password" -> r.password.asJson, "session" -> r.session.asJson
  )
  implicit val registerDecoder: Decoder[Register] = (c:HCursor) => for {
    email <- c.downField("email").as[String]
    pw <- c.downField("password").as[String]
    session <- c.downField("session").as[ActiveSession]
  } yield Register(email, pw, session)

  implicit val withSessionEncoder: Encoder[WithSession] = (ws:WithSession) => Json.obj(
    "session" -> ws.a.asJson, "call" -> ws.c.asJson
  )
  implicit val withSessionDecoder: Decoder[WithSession] = (c:HCursor) => for {
    session <- c.downField("session").as[ActiveSession]
    call <- c.downField("call").as[Call]
  } yield WithSession(session, call)

  implicit val loginEncoder: Encoder[Login] = (l:Login) => Json.obj(
    "email" -> l.email.asJson,
    "password" -> l.password.asJson,
    "session" -> l.session.asJson
  )
  implicit val loginDecoder: Decoder[Login] = (c:HCursor) => for {
    email <- c.downField("email").as[String]
    pw <- c.downField("password").as[String]
    session <- c.downField("session").as[ActiveSession]
  } yield Login(email, pw, session)

  implicit val createCourseEnc: Encoder[CreateCourse] = (c:CreateCourse) => Json.obj("course" -> c.c.asJson)
  implicit val createCourseDec: Decoder[CreateCourse] = (c:HCursor) => c.downField("course").as[Course].map(CreateCourse)

  implicit val createGroupSetEnc: Encoder[CreateGroupSet] = (c:CreateGroupSet) => Json.obj("groupSet" -> c.gs.asJson)
  implicit val createGroupSetDec: Decoder[CreateGroupSet] = (c:HCursor) => c.downField("groupSet").as[GroupSet].map(CreateGroupSet)

  implicit val createTaskEnc: Encoder[CreateTask] = (c:CreateTask) => Json.obj("task" -> c.t.asJson)
  implicit val createTaskDec: Decoder[CreateTask] = (c:HCursor) => c.downField("task").as[Task].map(CreateTask)

  implicit val createGroupsFromCsvEnc: Encoder[CreateGroupsFromCsv] = (c:CreateGroupsFromCsv) => Json.obj(
    "set" -> c.setId.asJson, "csv" -> c.csv.asJson
  )
  implicit val createGroupsFromCsvDec: Decoder[CreateGroupsFromCsv] = (c:HCursor) => for {
    set <- c.downField("set").as[Id[GroupSet, String]]
    csv <- c.downField("csv").as[String]
  } yield CreateGroupsFromCsv(set, csv)

  implicit val createGroupEnc: Encoder[CreateGroup] = (c:CreateGroup) => Json.obj("group" -> c.g.asJson)
  implicit val createGroupDec: Decoder[CreateGroup] = (c:HCursor) => c.downField("group").as[Group].map(CreateGroup)

  implicit val addGroupRegEnc: Encoder[AddGroupReg] = (a:AddGroupReg) => Json.obj("groupReg" -> a.gr.asJson)
  implicit val addGroupRegDec: Decoder[AddGroupReg] = (c:HCursor) => c.downField("groupReg").as[Group.Reg].map(AddGroupReg)

  implicit val callEncoder: Encoder[Call] = {
    case GetSession => Json.obj(k -> Json.fromString("GetSession"))
    case w:WithSession => Json.obj(k -> Json.fromString("WithSession"), b -> w.asJson)
    case r:Register => Json.obj(k -> Json.fromString("Register"), b -> r.asJson)
    case l:Login => Json.obj(k -> Json.fromString("Login"), b -> l.asJson)

    case c:CreateCourse => Json.obj(k -> Json.fromString("CreateCourse"), b -> c.asJson)
    case c:CreateTask => Json.obj(k -> Json.fromString("CreateTask"), b -> c.asJson)

    case c:CreateGroupSet => Json.obj(k -> Json.fromString("CreateGroupSet"), b -> c.asJson)
    case c:CreateGroup => Json.obj(k -> Json.fromString("CreateGroup"), b -> c.asJson)
    case c:AddGroupReg => Json.obj(k -> Json.fromString("AddGroupReg"), b -> c.asJson)
    case c:CreateGroupsFromCsv => Json.obj(k -> Json.fromString("CreateGroupsFromCsv"), b -> c.asJson)
  }

  implicit val callDecoder: Decoder[Call] = (c: HCursor) => {
    c.downField(k).as[String].flatMap {
      case "GetSession" => Right(GetSession)
      case "WithSession" => c.downField(b).as[WithSession]
      case "Register" => c.downField(b).as[Register]
      case "Login" => c.downField(b).as[Login]

      case "CreateCourse" => c.downField(b).as[CreateCourse]
      case "CreateTask" => c.downField(b).as[CreateTask]

      case "CreateGroupSet" => c.downField(b).as[CreateGroupSet]
      case "CreateGroup" => c.downField(b).as[CreateGroup]
      case "AddGroupReg" => c.downField(b).as[AddGroupReg]
      case "CreateGroupsFromCsv" => c.downField(b).as[CreateGroupsFromCsv]
    }
  }

  def write(c:Call): String = c.asJson.toString

  def readCall(text: String): Try[Call] = decode[Call](text).toTry

  def readCallF(text:String): Future[Call] = Future.fromTry(readCall(text))

  def readCallR(text:String): Ref[Call] = Ref(readCall(text))



  implicit val returnSessionEnc: Encoder[ReturnSession] = (r:ReturnSession) => Json.obj("session" -> r.a.asJson)
  implicit val returnSessionDec: Decoder[ReturnSession] = (c:HCursor) => c.downField("session").as[ActiveSession].map(ReturnSession)

  implicit val returnUserEnc: Encoder[ReturnUser] = (r:ReturnUser) => Json.obj("user" -> r.u.asJson)
  implicit val returnUserDec: Decoder[ReturnUser] = (c:HCursor) => c.downField("user").as[User].map(ReturnUser)

  implicit val returnCourseEnc: Encoder[ReturnCourse] = (r:ReturnCourse) => Json.obj("course" -> r.c.asJson)
  implicit val returnCourseDec: Decoder[ReturnCourse] = (c:HCursor) => c.downField("course").as[Course].map(ReturnCourse)

  implicit val returnTaskEnc: Encoder[ReturnTask] = (r:ReturnTask) => Json.obj("task" -> r.t.asJson)
  implicit val returnTaskDec: Decoder[ReturnTask] = (c:HCursor) => c.downField("task").as[Task].map(ReturnTask)

  implicit val returnGroupSetEnc: Encoder[ReturnGroupSet] = (r:ReturnGroupSet) => Json.obj("groupSet" -> r.gs.asJson)
  implicit val returnGroupSetDec: Decoder[ReturnGroupSet] = (c:HCursor) => c.downField("groupSet").as[GroupSet].map(ReturnGroupSet)

  implicit val returnGroupRegEnc: Encoder[ReturnGroupReg] = (r:ReturnGroupReg) => Json.obj("reg" -> r.gr.asJson)
  implicit val returnGroupRegDec: Decoder[ReturnGroupReg] = (c:HCursor) => c.downField("reg").as[Group.Reg].map(ReturnGroupReg)

  implicit val returnGroupsDataEnc: Encoder[ReturnGroupsData] = (r:ReturnGroupsData) => Json.obj("groups" -> r.data.asJson)
  implicit val returnGroupsDataDec: Decoder[ReturnGroupsData] = (c:HCursor) => c.downField("groups").as[Seq[(Group, Seq[String])]].map(ReturnGroupsData)


  implicit val returnEncoder: Encoder[Return] = {
    case r:ReturnSession => Json.obj(k -> Json.fromString("ReturnSession"), b -> r.asJson)
    case r:ReturnUser => Json.obj(k -> Json.fromString("ReturnUser"), b -> r.asJson)
    case r:ReturnCourse => Json.obj(k -> Json.fromString("ReturnCourse"), b -> r.asJson)
    case r:ReturnTask => Json.obj(k -> Json.fromString("ReturnTask"), b -> r.asJson)
    case r:ReturnGroupSet => Json.obj(k -> Json.fromString("ReturnGroupSet"), b -> r.asJson)
    case r:ReturnGroupReg => Json.obj(k -> Json.fromString("ReturnGroupReg"), b -> r.asJson)
    case r:ReturnGroupsData => Json.obj(k -> Json.fromString("ReturnGroupsData"), b -> r.asJson)
  }

  implicit val returnDecoder: Decoder[Return] = (c: HCursor) => {
    c.downField(k).as[String].flatMap {
      case "ReturnSession" => c.downField(b).as[ReturnSession]
      case "ReturnUser" => c.downField(b).as[ReturnUser]
      case "ReturnCourse" => c.downField(b).as[ReturnCourse]
      case "ReturnTask" => c.downField(b).as[ReturnTask]
      case "ReturnGroupSet" => c.downField(b).as[ReturnGroupSet]
      case "ReturnGroupReg" => c.downField(b).as[ReturnGroupReg]
      case "ReturnGroupsData" => c.downField(b).as[ReturnGroupsData]
    }
  }


  def write(r:Return): String = r.asJson.toString

  def readReturn(text: String): Try[Return] = decode[Return](text).toTry

  def readReturnF(text:String): Future[Return] = Future.fromTry(readReturn(text))

  def readReturnR(text:String): Ref[Return] = Ref(readReturn(text))

}
