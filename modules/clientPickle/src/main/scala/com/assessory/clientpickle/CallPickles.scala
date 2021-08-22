package com.assessory.clientpickle

import com.assessory.api.Task
import com.assessory.api.call._
import com.wbillingsley.handy.{Id, Ref}
import com.assessory.api.appbase._
import io.circe._
import io.circe.syntax._
import io.circe.parser.decode

import scala.concurrent.Future
import scala.util.Try

object CallPickles {

  import Pickles.{given, _}
  import UserCall._

  val k = "kind"
  val b = "body"


  //

  given Codec.AsObject[UserCall] = Codec.AsObject.derived
  given Codec.AsObject[SessionCall] = Codec.AsObject.derived
  given Codec.AsObject[CourseCall] = Codec.AsObject.derived
  given Codec.AsObject[TaskCall] = Codec.AsObject.derived
  given Codec.AsObject[TaskOutputCall] = Codec.AsObject.derived
  given Codec.AsObject[CritiqueCall] = Codec.AsObject.derived
  given Codec.AsObject[GroupSetCall] = Codec.AsObject.derived
  given Codec.AsObject[GroupCall] = Codec.AsObject.derived
  given Codec.AsObject[StandardReturn] = Codec.AsObject.derived

  implicit val callEncoder: Encoder[Call] = {
    case uc:UserCall => Json.obj(k -> Json.fromString("UserCall"), b -> uc.asJson)
    case sc:SessionCall => Json.obj(k -> Json.fromString("SessionCall"), b -> sc.asJson)
    case cc:CourseCall => Json.obj(k -> Json.fromString("CourseCall"), b -> cc.asJson)
    case tc:TaskCall => Json.obj(k -> Json.fromString("TaskCall"), b -> tc.asJson)
    case toc:TaskOutputCall => Json.obj(k -> Json.fromString("TaskOutputCall"), b -> toc.asJson)
    case cc:CritiqueCall => Json.obj(k -> Json.fromString("CritiqueCall"), b -> cc.asJson)
    case gsc:GroupSetCall => Json.obj(k -> Json.fromString("GroupSetCall"), b -> gsc.asJson)
    case gc:GroupCall => Json.obj(k -> Json.fromString("GroupCall"), b -> gc.asJson)
  }

  implicit val callDecoder: Decoder[Call] = (c: HCursor) => {
    c.downField(k).as[String].flatMap {
      case "UserCall" => c.downField(b).as[UserCall]
      case "SessionCall" => c.downField(b).as[SessionCall]
      case "CourseCall" => c.downField(b).as[CourseCall]
      case "TaskCall" => c.downField(b).as[TaskCall]
      case "TaskOutputCall" => c.downField(b).as[TaskOutputCall]
      case "CritiqueCall" => c.downField(b).as[CritiqueCall]
      case "GroupSetCall" => c.downField(b).as[GroupSetCall]
      case "GroupCall" => c.downField(b).as[GroupCall]
    }
  }

  def write(c:Call): String = c.asJson.toString

  def readCall(text: String): Try[Call] = decode[Call](text).toTry

  def readCallF(text:String): Future[Call] = Future.fromTry(readCall(text))

  def readCallR(text:String): Ref[Call] = Ref(readCall(text))



  implicit val returnSessionEnc: Encoder[ReturnSession] = (r:ReturnSession) => Json.obj("session" -> r.a.asJson)
  implicit val returnSessionDec: Decoder[ReturnSession] = (c:HCursor) => c.downField("session").as[ActiveSession].map(ReturnSession.apply)

  implicit val returnUserEnc: Encoder[ReturnUser] = (r:ReturnUser) => Json.obj("user" -> r.u.asJson)
  implicit val returnUserDec: Decoder[ReturnUser] = (c:HCursor) => c.downField("user").as[User].map(ReturnUser.apply)

  implicit val returnCourseEnc: Encoder[ReturnCourse] = (r:ReturnCourse) => Json.obj("course" -> r.c.asJson)
  implicit val returnCourseDec: Decoder[ReturnCourse] = (c:HCursor) => c.downField("course").as[Course].map(ReturnCourse.apply)

  implicit val returnTaskEnc: Encoder[ReturnTask] = (r:ReturnTask) => Json.obj("task" -> r.t.asJson)
  implicit val returnTaskDec: Decoder[ReturnTask] = (c:HCursor) => c.downField("task").as[Task].map(ReturnTask.apply)

  implicit val returnGroupSetEnc: Encoder[ReturnGroupSet] = (r:ReturnGroupSet) => Json.obj("groupSet" -> r.gs.asJson)
  implicit val returnGroupSetDec: Decoder[ReturnGroupSet] = (c:HCursor) => c.downField("groupSet").as[GroupSet].map(ReturnGroupSet.apply)

  implicit val returnGroupRegEnc: Encoder[ReturnGroupReg] = (r:ReturnGroupReg) => Json.obj("reg" -> r.gr.asJson)
  implicit val returnGroupRegDec: Decoder[ReturnGroupReg] = (c:HCursor) => c.downField("reg").as[Group.Reg].map(ReturnGroupReg.apply)

  implicit val returnGroupsDataEnc: Encoder[ReturnGroupsData] = (r:ReturnGroupsData) => Json.obj("groups" -> r.data.asJson)
  implicit val returnGroupsDataDec: Decoder[ReturnGroupsData] = (c:HCursor) => c.downField("groups").as[Seq[(Group, Seq[String])]].map(ReturnGroupsData.apply)


  given Codec.AsObject[ReturnGroup] = Codec.AsObject.derived
  given Codec.AsObject[ReturnTaskOutput] = Codec.AsObject.derived
  given Codec.AsObject[ReturnTarget] = Codec.AsObject.derived

  implicit val returnEncoder: Encoder[Return] = {
    case s:StandardReturn => Json.obj(k -> Json.fromString("StandardReturn"), b -> s.asJson)

    case r:ReturnSession => Json.obj(k -> Json.fromString("ReturnSession"), b -> r.asJson)
    case r:ReturnUser => Json.obj(k -> Json.fromString("ReturnUser"), b -> r.asJson)
    case r:ReturnCourse => Json.obj(k -> Json.fromString("ReturnCourse"), b -> r.asJson)
    case r:ReturnTask => Json.obj(k -> Json.fromString("ReturnTask"), b -> r.asJson)
    case r:ReturnTaskOutput => Json.obj(k -> Json.fromString("ReturnTaskOutput"), b -> r.asJson)
    case r:ReturnTarget => Json.obj(k -> Json.fromString("ReturnTarget"), b -> r.asJson)
    case r:ReturnGroup => Json.obj(k -> Json.fromString("ReturnGroup"), b -> r.asJson)
    case r:ReturnGroupSet => Json.obj(k -> Json.fromString("ReturnGroupSet"), b -> r.asJson)
    case r:ReturnGroupReg => Json.obj(k -> Json.fromString("ReturnGroupReg"), b -> r.asJson)
    case r:ReturnGroupsData => Json.obj(k -> Json.fromString("ReturnGroupsData"), b -> r.asJson)
  }

  implicit val returnDecoder: Decoder[Return] = (c: HCursor) => {
    c.downField(k).as[String].flatMap {
      case "StandardReturn" => c.downField(b).as[StandardReturn]

      case "ReturnSession" => c.downField(b).as[ReturnSession]
      case "ReturnUser" => c.downField(b).as[ReturnUser]
      case "ReturnCourse" => c.downField(b).as[ReturnCourse]
      case "ReturnTask" => c.downField(b).as[ReturnTask]
      case "ReturnTaskOutput" => c.downField(b).as[ReturnTaskOutput]
      case "ReturnTarget" => c.downField(b).as[ReturnTarget]
      case "ReturnGroupSet" => c.downField(b).as[ReturnGroupSet]
      case "ReturnGroup" => c.downField(b).as[ReturnGroup]
      case "ReturnGroupReg" => c.downField(b).as[ReturnGroupReg]
      case "ReturnGroupsData" => c.downField(b).as[ReturnGroupsData]
    }
  }


  def write(r:Return): String = r.asJson.toString

  def readReturn(text: String): Try[Return] = decode[Return](text).toTry

  def readReturnF(text:String): Future[Return] = Future.fromTry(readReturn(text))

  def readReturnR(text:String): Ref[Return] = Ref(readReturn(text))

}
