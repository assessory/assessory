package org.assessory.play.controllers

import com.assessory.api.client.WithPerms
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import Pickles._
import com.assessory.model.GroupModel
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{Course, Group, GroupSet, UserError}
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import util.RefConversions._
import util.UserAction

import scala.concurrent.Future
import scala.language.implicitConversions

class GroupController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)
  extends AbstractController(cc) {

  implicit def groupToResult(rc:Ref[Group]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def groupSetToResult(rc:Ref[GroupSet]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def wpgToResult(rc:Ref[WithPerms[Group]]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def wpgsToResult(rc:Ref[WithPerms[GroupSet]]):Future[Result] = {
    rc.map(c => Results.Ok(Pickles.write(c)).as("application/json")).toFuture
  }

  implicit def manyGroupToResult(rc:RefMany[Group]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyGroupSetToResult(rc:RefMany[GroupSet]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWpgToResult(rc:RefMany[WithPerms[Group]]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }

  implicit def manyWpgsToResult(rc:RefMany[WithPerms[GroupSet]]):Future[Result] = {
    val strings = rc.map(c => Pickles.write(c))

    for {
      j <- strings.jsSource
    } yield Results.Ok.chunked(j).as("application/json")
  }


  def groupSet(id:String) = userAction.async { implicit request =>
    GroupModel.groupSet(request.approval, id.asId)
  }

  def createGroupSet(courseId:String) = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to create group set had no body to parse")
      clientGS <- Pickles.read[GroupSet](text).toRef
      wp <- GroupModel.createGroupSet(request.approval, clientGS)
    } yield wp

    wp
  }

  def editGroupSet(gsId:String) = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef orFail UserError("Request to edit group set had no body to parse")
      clientGS <- Pickles.read[GroupSet](text).toRef
      wp <- GroupModel.editGroupSet(request.approval, clientGS).require
    } yield wp

    wp
  }

  /**
   * The group sets in a course
   */
  def courseGroupSets(courseId:String) = userAction.async { implicit request =>
    GroupModel.courseGroupSets(
      a=request.approval,
      rCourse=LazyId(courseId).of[Course]
    )
  }

  /**
   * Creates a group pre-enrolment from submitted CSV data
   * groupName,service,id,username
   *
    * def createGroupSetPreenrol(gsId:String) = DataAction.returning.oneWH(parse.json) { implicit request =>
    * WithHeaderInfo(
    * GroupModel.createGroupSetPreenrol(
    * a=request.approval,
    * rGS=LazyId(gsId).of[GroupSet],
    * oCsv=(request.body \ "csv").asOpt[String]
    * ),
    * headerInfo
    * )
    * }*/

  def testCsv = userAction.async(parse.tolerantText) { implicit request =>
    import java.io.StringReader

    import au.com.bytecode.opencsv.CSVReader
    import com.wbillingsley.handyplay.RefConversions._

    import scala.collection.JavaConverters._

    val reader = new CSVReader(new StringReader(request.body.trim()))
    val lines = reader.readAll().asScala.toSeq.toRefMany.map(_.toSeq.toString)

    for {
      e <- lines.toFutureSource
    } yield Ok.chunked(e).as("application/csv")
  }

  /**
   * Creates groups from submitted CSV data
   *
    * def importFromCsv(gsId:String) = DataAction.returning.manyWH(parse.tolerantText) { implicit request =>
    * WithHeaderInfo(
    * GroupModel.importFromCsv(
    * a=request.approval,
    * rSet=LazyId(gsId).of[GroupSet],
    * csv=request.body
    * ),
    * headerInfo
    * )
    * }

    * def uploadGroups(gs:Ref[GroupSet]) = DataAction.returning.manyWH(parse.json) { implicit request =>
    * WithHeaderInfo(
    * (request.body \ "csv").asOpt[String].toRef flatMap { csv =>
    * GroupModel.importFromCsv(
    * a=request.approval,
    * rSet=gs,
    * csv=csv
    * )
    * },
    * headerInfo
    * )
    * }*/

  /**
   * The groups belonging to a particular group set
   */
  def groupSetGroups(gsId:String) = userAction.async { implicit request =>
    GroupModel.groupSetGroups(
      a=request.approval,
      rGS=LazyId(gsId).of[GroupSet]
    )
  }

  def group(id:String) = userAction.async { implicit request =>
    GroupModel.group(request.approval, id.asId)
  }

  def myGroups = userAction.async { implicit request =>
    GroupModel.myGroupsWP(
      a=request.approval
    )
  }

  def myGroupsInCourse(courseId:String) = userAction.async { implicit request =>
    GroupModel.myGroupsInCourseWP(
      a=request.approval,
      rCourse=LazyId(courseId).of[Course]
    )
  }

  def findMany = userAction.async { implicit request =>
    def wp = for {
      text <- request.body.asText.toRef
      ids <- Pickles.read[Ids[Group,String]](text).toRef
      wp <- GroupModel.findMany(request.approval, ids)
    } yield wp

    wp
  }
}
