package org.assessory.play.controllers

import com.assessory.api.video.{SmallFile, SmallFileDetails}
import com.assessory.asyncmongo.SmallFileDAO
import com.assessory.model.CourseModel
import com.wbillingsley.handy.{Id, Ref, Approval}
import com.wbillingsley.handy.appbase.{Course, User}
import Ref._
import Id._
import play.api.mvc.Controller
import util.UserAction

import com.assessory.api.wiring.Lookups._

import scala.util.Try

/**
  * Created by wbilling on 15/02/2017.
  */
class SmallFileController extends Controller {


  def uploadFile(courseId:String) = UserAction.async(parse.temporaryFile) { implicit request =>

    def wp = for {
      u <- request.approval.who
      c <- courseId.asId[Course].lazily
      reg <- CourseModel.myRegistrationInCourse(u.itself, c.itself)
      id=SmallFileDAO.allocateId.asId[SmallFile]
      data <- Try { java.nio.file.Files.readAllBytes(request.body.file.toPath) }.toRef
      f = SmallFile(
          id=id,
          details=SmallFileDetails(
            id=id, courseId=c.id, ownerId=reg.id,
            name=request.headers.get("X-FILENAME").getOrElse(request.body.file.getName), size=Some(data.length), mime=request.contentType,
            created=System.currentTimeMillis(),
            updated=System.currentTimeMillis()
          ),
          data= data
        )
      saved <- SmallFileDAO.saveSafe(f)
    } yield {
      Ok(upickle.default.write(saved.details)).as("application/json")
    }

    wp.toFuture
  }

  def downloadFile(fileId:String) = UserAction.async { implicit request =>

    def result = for {
      f <- SmallFileDAO.byId(fileId)
    } yield Ok(f.data).as(f.details.name)

    result.toFuture
  }

  def getDetails(fileId:String) = UserAction.async { implicit request =>
    def result = for {
      f <- SmallFileDAO.getDetails(fileId.asId)
    } yield Ok(upickle.default.write(f)).as("application/json")

    result.toFuture

  }

}
