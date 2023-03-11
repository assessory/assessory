package org.assessory.play.cheatscript

import com.wbillingsley.handy.{Ref, RefFailed, RefOptFailed, RefSome, RefNone, RefOpt, RefItself, refOps}
import com.assessory.api.*
import call.*
import appbase.*
import com.assessory.api.client.WithPerms
import com.assessory.api.question.{QuestionId, QuestionnaireTask, VideoQuestion}
import com.assessory.clientpickle.CallClient

import scala.concurrent.Future

object App {

  def main(args:Array[String]): Unit = {

    println("Starting...")
    val networkService = NetworkService("https://assessory.une.edu.au/api/call")
    import networkService.given
    val client = CallClient()
    println("Created client...")

    def createTestUser() = for u <- client.register("test@example.com", "samplepassword").toRef yield println(u)
    def loginTestUser() = for u <- client.login("test@example.com", "samplepassword").toRef yield u

    def ensureTestUser() = loginTestUser().toRefOpt orElse createTestUser()

    def admin() = (for u <- client.login("admin@assessory.une.edu.au", "samplepassword").toRef yield u)

    def makeTestCourse():Ref[WithPerms[Course]] = (for
      StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- client.makeCall(CourseCall.CreateCourse(Course(
        id = CourseId("61147cf6dbdf4746b7012183"),
        addedBy = RegistrationId("invalid"),
        title = Some("A Testing Course"),
        shortName = Some("TEST100"),
        shortDescription = Some("In which we test if the system is working"),
        ltis = Seq(LTIConsumer("UNE moodle", "grumplestiltskin"))
      ))).toRef
    yield WithPerms(perms, c)) orFail IllegalStateException("Failed to create course")

    def getTestCourse():RefOpt[WithPerms[Course]] =
      for
        StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- client.makeCall(
          CourseCall.GetCourse(CourseId("61147cf6dbdf4746b7012183"))
        ).toRef
      yield WithPerms(perms, c)

    def ensureTestCourse() = getTestCourse() orElse makeTestCourse()

    def makeTestTask() =
      (for
        WithPerms(_, c) <- ensureTestCourse()
        StandardReturn.ReturnWithPermissions(ReturnTask(t), perms) <- client.makeCall(TaskCall.CreateTask(
          Task(
            id = TaskId("611c7e8e90a80c6b9ea520d6"),
            course = c.id,
            TaskDetails(
              name = Some("Progress video"),
              description = Some("Here is a description..."),
            ),
            QuestionnaireTask(Seq(
              VideoQuestion(id = QuestionId("61147cf6dbdf4746b7000001"),
                prompt = "Please post the embed code of your video below..."
              )
            ))
          )
        )).toRef
      yield WithPerms(perms, t)) orFail IllegalStateException("Failed creating test task")

    def getTestTask() =
      for
        StandardReturn.ReturnWithPermissions(ReturnTask(c), perms) <- client.makeCall(
          TaskCall.GetTask(TaskId("611c7e8e90a80c6b9ea520d6"))
        ).toRef
      yield WithPerms(perms, c)

    def ensureTestTask() = getTestTask() orElse makeTestTask()

    /*
    (for
      u <- ensureTestUser()
      _ = println("Logged in..")

      WithPerms(perms, c) <- ensureTestCourse()
      _ = println("Ensured test course exists")

      WithPerms(perms, t) <- makeTestTask()
      _ = println("made the test task")
    yield
      println(t)
      println(perms)
    ).recoverWith { case x:Throwable =>
      x.printStackTrace()
      RefFailed(x)
    }*/

    (for
      u <- admin()
      _ = println("Logged in..")

      _ <- Cosc220.run()(using client)
    yield
      println("Completed")
    ).recoverWith { case x:Throwable =>
      println("Error")
      x.printStackTrace()
      RefFailed(x)
    }

  }

}
