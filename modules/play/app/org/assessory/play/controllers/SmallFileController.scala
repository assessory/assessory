package org.assessory.play.controllers

import com.assessory.api.video.{SmallFile, SmallFileDetails}
import com.assessory.asyncmongo.SmallFileDAO
import com.assessory.model.CourseModel
import com.wbillingsley.handy.{Approval, Id, Ref, Refused}
import com.wbillingsley.handy.appbase.{Course, User}
import Ref._
import Id._
import play.api.mvc.{AbstractController, Controller, ControllerComponents}
import util.UserAction
import com.assessory.api.wiring.Lookups._
import com.assessory.clientpickle.Pickles
import Pickles._
import javax.inject.Inject

import scala.util.Try

/**
  * Created by wbilling on 15/02/2017.
  */
class SmallFileController @Inject() (startupSettings: StartupSettings, cc: ControllerComponents, userAction: UserAction)
  extends AbstractController(cc) {


  def uploadFile(courseId:String) = userAction.async(parse.temporaryFile) { implicit request =>

    def wp = for {
      u <- request.approval.who orFail Refused("You must be logged in")
      c <- courseId.asId[Course].lazily
      reg <- CourseModel.myRegistrationInCourse(u.itself, c.itself) orFail Refused("You are not a member of this course")
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
      Ok(Pickles.write(saved.details)).as("application/json")
    }

    wp.toFuture
  }

  def downloadFile(fileId:String) = userAction.async { implicit request =>

    def result = for {
      f <- SmallFileDAO.byId(fileId).require
    } yield Ok(f.data).as(f.details.name)

    result.toFuture
  }

  def getDetails(fileId:String) = userAction.async { implicit request =>
    def result = for {
      f <- SmallFileDAO.getDetails(fileId.asId)
    } yield Ok(Pickles.write(f)).as("application/json")

    result.toFuture

  }

}
