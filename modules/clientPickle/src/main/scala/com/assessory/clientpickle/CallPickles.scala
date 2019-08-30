package com.assessory.clientpickle

import com.assessory.api.call._
import com.wbillingsley.handy.Ref
import com.wbillingsley.handy.appbase.{Course, User}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.parser.decode

import scala.concurrent.Future
import scala.util.Try

object CallPickles {

  import Pickles._

  val k = "kind"
  val b = "body"

  implicit val registerEncoder: Encoder[Register] = deriveEncoder
  implicit val registerDecoder: Decoder[Register] = deriveDecoder

  implicit val withSessionEncoder: Encoder[WithSession] = deriveEncoder
  implicit val withSessionDecoder: Decoder[WithSession] = deriveDecoder

  implicit val loginEncoder: Encoder[Login] = deriveEncoder
  implicit val loginDecoder: Decoder[Login] = deriveDecoder

  implicit val createCourseEnc: Encoder[CreateCourse] = deriveEncoder
  implicit val createCourseDec: Decoder[CreateCourse] = deriveDecoder

  implicit val createGroupSetEnc: Encoder[CreateGroupSet] = deriveEncoder
  implicit val createGroupSetDec: Decoder[CreateGroupSet] = deriveDecoder

  implicit val createTaskEnc: Encoder[CreateTask] = deriveEncoder
  implicit val createTaskDec: Decoder[CreateTask] = deriveDecoder

  implicit val createGroupsFromCsvEnc: Encoder[CreateGroupsFromCsv] = deriveEncoder
  implicit val createGroupsFromCsvDec: Decoder[CreateGroupsFromCsv] = deriveDecoder

  implicit val createGroupEnc: Encoder[CreateGroup] = deriveEncoder
  implicit val createGroupDec: Decoder[CreateGroup] = deriveDecoder

  implicit val addGroupRegEnc: Encoder[AddGroupReg] = deriveEncoder
  implicit val addGroupRegDec: Decoder[AddGroupReg] = deriveDecoder

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



  implicit val returnSessionEnc: Encoder[ReturnSession] = deriveEncoder
  implicit val returnSessionDec: Decoder[ReturnSession] = deriveDecoder

  implicit val returnUserEnc: Encoder[ReturnUser] = deriveEncoder
  implicit val returnUserDec: Decoder[ReturnUser] = deriveDecoder

  implicit val returnCourseEnc: Encoder[ReturnCourse] = deriveEncoder
  implicit val returnCourseDec: Decoder[ReturnCourse] = deriveDecoder

  implicit val returnTaskEnc: Encoder[ReturnTask] = deriveEncoder
  implicit val returnTaskDec: Decoder[ReturnTask] = deriveDecoder

  implicit val returnGroupSetEnc: Encoder[ReturnGroupSet] = deriveEncoder
  implicit val returnGroupSetDec: Decoder[ReturnGroupSet] = deriveDecoder

  implicit val returnGroupRegEnc: Encoder[ReturnGroupReg] = deriveEncoder
  implicit val returnGroupRegDec: Decoder[ReturnGroupReg] = deriveDecoder

  implicit val returnGroupsDataEnc: Encoder[ReturnGroupsData] = deriveEncoder
  implicit val returnGroupsDataDec: Decoder[ReturnGroupsData] = deriveDecoder


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
