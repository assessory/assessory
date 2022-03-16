package org.assessory.play.cheatscript

import com.assessory.api.*
import com.assessory.api.appbase.*
import com.assessory.api.call.*
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.*
import com.assessory.api.due.DueDate
import com.assessory.api.question.*
import com.assessory.asyncmongo.TaskDAO
import com.assessory.clientpickle.CallClient
import com.wbillingsley.handy.{Ref, RefFailed, RefItself, RefNone, RefOpt, RefOptFailed, RefSome, refOps}

object Cosc360 {

  def createCourse()(using cc:CallClient):Ref[Course] =
    (for
      StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- cc.call(CourseCall.CreateCourse(Course(
        id=CourseId("invalid"),
        addedBy=RegistrationId("invalid"),
        title = Some("Advanced Web Programming"),
        shortName = Some("COSC360/560 2021"),
        shortDescription = Some("In which our heroes develop amazing web applications..."),
        ltis = Seq(LTIConsumer("UNE moodle", "grumplestiltskin"))
      )))
    yield c) orFail IllegalStateException("Return from creating course was not what I expected")

  def getCourse()(using cc:CallClient):RefOpt[Course] = (for ReturnCourse(c) <- cc.call(CourseCall.ByShortName("COSC360/560 2021")) yield c)

  def ensureCourse()(using cc:CallClient) = getCourse() orElse createCourse()

  def ensureTask(task:Task)(using cc:CallClient):Ref[Task] =
    def getTask() = for
      name <- task.details.name.toRefOpt.require
      ReturnTask(t) <- cc.call(TaskCall.ByName(task.course, name))
    yield t

    def makeTask() = (for
      StandardReturn.ReturnWithPermissions(ReturnTask(t), _) <- cc.call(TaskCall.CreateTask(task))
    yield t) orFail IllegalStateException("Return from creating task wasn't what I expected")

    getTask() orElse makeTask()


/*
  def fixup()(using cc:CallClient) = {
    for
      c <- getCourse()
      ReturnTask(critTask) <- cc.call(TaskCall.ByName(c.id, "Critique three videos"))
      _ = println(s"Got crit task with ID $critTask")

      updateCritTask = critTask.copy(details = critTask.details.copy(description = Some(
        """This task should allocate you 3 other videos to critique. You can offer a critique via video or via text.
          |""".stripMargin)))

      savedCritTask <- cc.call(TaskCall.)

    yield
      println("Done")
  }*/

  def run()(using cc:CallClient) =
    for
      course <- ensureCourse()
      _ = println(s"Course has id ${course.id}")

      progressVideo <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Assessment 3 Video"),
          open = DueDate(System.currentTimeMillis()),
          groupSet = None,
          individual = true,
          description = Some(
            """Please upload your video to either UNE MyMedia (Kaltura) or YouTube. If you use YouTube, ensure the
              |video is not private (or it can't be watched by your critics) - "unlisted" is ok.
              |
              |Paste the embed code (for Kaltura videos) or the video URL (for YouTube videos) below. If the code or
              |URL is recognised, you'll see a preview of the video appear when you click "preview". Then click save.
              |
              |Don't forget to click "Publish" or "Make available" once you are done. This doesn't stop you from
              |editing the form, but does make it available to students for critique.
              |""".stripMargin)
        ),
        body = QuestionnaireTask(Seq(
          VideoQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt =
              """Progress video
                |""".stripMargin
          )
        ))
      ))
      _ = println(s"Video task has id ${progressVideo.id}")


      critique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Critique three videos"),
          open = DueDate(System.currentTimeMillis()),
          groupSet = None,
          individual = true,
          description = Some(
            """This task should allocate you 3 other groups' progress videos to critique. It is an individual task -
              |you don't critique as a group. You can offer a critique via video or via text.
              |""".stripMargin),
          restrictions = Seq(MustHaveFinished(progressVideo.id))
        ),
        body = CritiqueTask(
          strategy = AllocateStrategy(TTOutputs(progressVideo.id), 3),
          task = QuestionnaireTask(Seq(
            VideoQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt =
                """If you are giving feedback via video, paste the MyMedia embed code or YouTube video url below.
                  |""".stripMargin
            ),
            ShortTextQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt =
                """If you are giving feedback via text, type it here.
                  |""".stripMargin
            )
          ))
        ),
      ))
      _ = println(s"Crit task has id ${critique.id}")

      reverseCritique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("View your critiques"),
          open = DueDate(System.currentTimeMillis()),
          groupSet = None,
          individual = true,
          description = Some(
            """This task should show you all the critiques your group has received. You're asked to fill in a little
              |form on whether they were helpful, constructive, etc.
              |""".stripMargin),
          restrictions = Seq(MustHaveFinished(progressVideo.id))
        ),
        body = CritiqueTask(
          strategy = TargetMyStrategy(critique.id, TTOutputs(progressVideo.id), None),
          task = QuestionnaireTask(Seq(
            BooleanQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "Was the feedback constructive?"
            ),
            BooleanQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "Was the feedback actionable?"
            ),
            BooleanQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "Was the feedback specific?"
            ),
            BooleanQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "Was the feedback helpful?"
            )
          ))
        )
      ))
      _ = println(s"Reverse task has id ${reverseCritique.id}")

    yield
      println("Done")

}
