package com.assessory.play.cheatscript


import com.assessory.api.{Task, TaskDetails, TaskId}
import com.assessory.asyncmongo.{DB, RegistrationDAO, UserDAO}
import com.assessory.model.{CourseModel, DoWiring, TaskModel, UserModel}
import com.wbillingsley.handy.{Approval, Ref, RefSome, refOps}
import com.assessory.api.appbase.{ActiveSession, Course, CourseId, LTIConsumer, RegistrationId, User, UserId}
import com.wbillingsley.handy.Id.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.*
import flatspec.*
import matchers.*

import scala.util.Success

class Courses2016Spec extends AnyFlatSpec with should.Matchers with ScalaFutures {

  "setting up the courses" should "succeed" in {

    DB.dbName = "assessory_2016_1"
    // Wire up the lookups
    DoWiring.doWiring

    val proc = for {
      will <- UserModel.signUp(
        Some("wbilling@une.edu.au"),
        Some("Aalurfwayaf"),
        ActiveSession("locally made", "127.0.0.1")
      )

      testCourseWp <- CourseModel.create(
        Approval(RefSome(will)), Course(
          id = CourseId("570fa8c4470b6bd820000000"),
          addedBy = RegistrationId("invalid"),
          title = Some("test course"),
          shortName = Some("TEST101"),
          ltis = Seq(LTIConsumer("UNE moodle", "grumplestiltskin", Some("Term 1 at UNE")))
        )
      )

      videoTask <- TaskModel.create(Approval(RefSome(will)),
        Task(
          id = TaskId("invalid"),
          course = testCourseWp.item.id,
          details = TaskDetails()
        )
      )
    } yield "success"

    proc.toFuture.futureValue should be ("success")

  }

}
