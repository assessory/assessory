package org.assessory.play.cheatscript

import com.assessory.api.{MustHaveFinished, Task, TaskDetails, TaskId}
import com.assessory.api.appbase.{Course, CourseId, LTIConsumer, RegistrationId}
import com.assessory.api.call.{CourseCall, ReturnCourse, ReturnTask, StandardReturn, TaskCall}
import com.assessory.api.critique.{AllocateStrategy, CritiqueTask, TTOutputs, TargetMyStrategy}
import com.assessory.api.due.DueDate
import com.assessory.api.question.{BooleanQuestion, QuestionId, QuestionnaireTask, ShortTextQuestion, VideoQuestion}
import com.assessory.asyncmongo.TaskDAO
import com.assessory.clientpickle.CallClient
import com.wbillingsley.handy.{Ref, RefOpt, refOps}
import java.time.LocalDate
import java.time.ZoneId

/**
  * Created by wbilling on 21/02/2017.
  */
object Cosc370 {

  val zone = ZoneId.of("Australia/Sydney")

  def openDate(year:Int, month:Int, day:Int) = DueDate(LocalDate.of(year, month, day).atStartOfDay.atZone(zone).toInstant().toEpochMilli())
  def closeDate(year:Int, month:Int, day:Int) = DueDate(LocalDate.of(year, month, day).atTime(23, 59, 59, 999999999).atZone(zone).toInstant().toEpochMilli())

  def createCourse()(using cc:CallClient):Ref[Course] =
    (for
      case StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- cc.call(CourseCall.CreateCourse(Course(
        id=CourseId("invalid"),
        addedBy=RegistrationId("invalid"),
        title = Some("User Experience and Interaction Design"),
        shortName = Some("COSC370/570 2024"),
        shortDescription = Some("In which our heroes practice design thinking..."),
        ltis = Seq(LTIConsumer("UNE moodle", "grumplestiltskin"))
      )))
    yield c) orFail IllegalStateException("Return from creating course was not what I expected")

  def getCourse()(using cc:CallClient):RefOpt[Course] = (for case ReturnCourse(c) <- cc.call(CourseCall.ByShortName("COSC370/570 2024")) yield c)

  def ensureCourse()(using cc:CallClient) = getCourse() orElse createCourse()

  def ensureTask(task:Task)(using cc:CallClient):Ref[Task] =
    def getTask() = for
      name <- task.details.name.toRefOpt.require
      case ReturnTask(t) <- cc.call(TaskCall.ByName(task.course, name))
    yield t

    def makeTask() = (for
      case StandardReturn.ReturnWithPermissions(ReturnTask(t), _) <- cc.call(TaskCall.CreateTask(task))
    yield t) orFail IllegalStateException("Return from creating task wasn't what I expected")

    getTask() orElse makeTask()

  def run()(using cc:CallClient) =
    for
      course <- ensureCourse()
      _ = println(s"Course has id ${course.id}")

      conceptVideo <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Concept Video"),
          open = openDate(2024, 3, 16),
          closed = closeDate(2024, 4, 9),
          groupSet = None,
          individual = true,
          description = Some(
            """Paste the public share link of your video below. 
              |To obtain this, go to your EchoVideo library, find your video, go to the "Share" settings, the "Links" tab, create a new public share link, and press the copy-to-clipboard button.
              |
              |When you paste the public share link in below, if the URL is recognised, you'll see a preview of the video appear when you click "preview". Then click save.
              |
              |In case of emergency, this system can also recognise YouTube public or unlisted video URLs. i.e. you can re-upload your video to YouTube and share that version here.
              |Note that a YouTube video needs to be public or unlisted, but not private. (Otherwise you'll be able to see it but your critics and the marker won't.)
              |
              |Don't forget to click "Publish" once you are done. This doesn't stop you from editing the form, but does make it available to students for critique.
              |""".stripMargin)
        ),
        body = QuestionnaireTask(Seq(
          VideoQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt =
              """Concept video
                |""".stripMargin
          ),
          ShortTextQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt = "(Optional, not shown in critique) What was hardest about your work ...",
            hideInCrit = true
          )
        ))
      ))
      _ = println(s"Concept video task has id ${conceptVideo.id}")


      conceptCritique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Concept stage: Critique three videos"),
          open = openDate(2024, 3, 19),
          groupSet = None,
          individual = true,
          description = Some(
            """This task should allocate you 3 other students' concept videos to critique. The videos to critique
              |are allocated as you access this form, selecting the three least-allocated videos. (Though if you're
              |one of the first to access it, there might not be three available yet - in which case you'll need to revisit
              |this task later and it will automatically fill up your allocation.)
              |""".stripMargin),
          restrictions = Seq(MustHaveFinished(conceptVideo.id))
        ),
        body = CritiqueTask(
          strategy = AllocateStrategy(TTOutputs(conceptVideo.id), 3),
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
            ),
            ShortTextQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "(Optional, not shown in critique) What was hardest about your work ...",
              hideInCrit = true
            )
          ))
        ),
      ))
      _ = println(s"Crit task has id ${conceptCritique.id}")

      conceptReverseCritique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Concept stage: View your critiques"),
          open = openDate(2024, 3, 20),
          groupSet = None,
          individual = true,
          description = Some(
            """This task should show you all the critiques your video has received. You're asked to fill in a little
              |form on whether they were helpful, constructive, etc.
              |""".stripMargin),
          restrictions = Seq(MustHaveFinished(conceptVideo.id))
        ),
        body = CritiqueTask(
          strategy = TargetMyStrategy(conceptCritique.id, TTOutputs(conceptVideo.id), None),
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
            ),
            ShortTextQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "(Optional, not shown in marking) What did you find most useful in the critique ...",
              hideInCrit = true
            )
          ))
        )
      ))
      _ = println(s"Reverse task has id ${conceptReverseCritique.id}")

      designVideo <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Design Prototype Video"),
          open = openDate(2024, 4, 10),
          groupSet = None,
          individual = true,
          description = Some(            
            """Paste the public share link of your video below. 
              |To obtain this, go to your EchoVideo library, find your video, go to the "Share" settings, the "Links" tab, create a new public share link, and press the copy-to-clipboard button.
              |
              |When you paste the public share link in below, if the URL is recognised, you'll see a preview of the video appear when you click "preview". Then click save.
              |
              |In case of emergency, this system can also recognise YouTube public or unlisted video URLs. i.e. you can re-upload your video to YouTube and share that version here.
              |Note that a YouTube video needs to be public or unlisted, but not private. (Otherwise you'll be able to see it but your critics and the marker won't.)
              |
              |Don't forget to click "Publish" once you are done. This doesn't stop you from editing the form, but does make it available to students for critique.
              |""".stripMargin)
        ),
        body = QuestionnaireTask(Seq(
          VideoQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt =
              """Design prototype video
                |""".stripMargin
          ),
          ShortTextQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt = "(Optional, not shown in critique) What was hardest about your work ...",
            hideInCrit = true
          )
        ))
      ))
      _ = println(s"Design prototype video task has id ${designVideo.id}")


      designCritique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Design prototype stage: Critique three videos"),
          open = openDate(2024, 4, 16),
          groupSet = None,
          individual = true,
          description = Some(
            """This task should allocate you 3 other students' design prototype videos to critique. The videos to critique
              |are allocated as you access this form, selecting the three least-allocated videos. (Though if you're
              |one of the first to access it, there might not be three available yet - in which case you'll need to revisit
              |this task later and it will automatically fill up your allocation.)
              |""".stripMargin),
          restrictions = Seq(MustHaveFinished(conceptVideo.id))
        ),
        body = CritiqueTask(
          strategy = AllocateStrategy(TTOutputs(designVideo.id), 3),
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
            ),
            ShortTextQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "(Optional, not shown in critique) What was hardest about your work ...",
              hideInCrit = true
            )
          ))
        ),
      ))
      _ = println(s"Crit task has id ${designCritique.id}")

      designReverseCritique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Design prototype stage: View your critiques"),
          open = openDate(2024, 4, 17),
          groupSet = None,
          individual = true,
          description = Some(
            """This task should show you all the critiques your video has received. You're asked to fill in a little
              |form on whether they were helpful, constructive, etc.
              |""".stripMargin),
          restrictions = Seq(MustHaveFinished(designVideo.id))
        ),
        body = CritiqueTask(
          strategy = TargetMyStrategy(designCritique.id, TTOutputs(designVideo.id), None),
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
            ),
            ShortTextQuestion(
              QuestionId(TaskDAO.allocateId),
              prompt = "(Optional, not shown in marking) What did you find most useful in the critique ...",
              hideInCrit = true
            )
          ))
        )
      ))
      _ = println(s"Reverse task has id ${designReverseCritique.id}")

      workingVideo <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Working Prototype Video"),
          open = openDate(2024, 5, 20),
          groupSet = None,
          individual = true,
          description = Some(
            """Paste the public share link of your video below. 
              |To obtain this, go to your EchoVideo library, find your video, go to the "Share" settings, the "Links" tab, create a new public share link, and press the copy-to-clipboard button.
              |
              |When you paste the public share link in below, if the URL is recognised, you'll see a preview of the video appear when you click "preview". Then click save.
              |
              |In case of emergency, this system can also recognise YouTube public or unlisted video URLs. i.e. you can re-upload your video to YouTube and share that version here.
              |Note that a YouTube video needs to be public or unlisted, but not private. (Otherwise you'll be able to see it but your critics and the marker won't.)
              |
              |Don't forget to click "Publish" once you are done. This doesn't stop you from editing the form, but does make it available to students for critique.
              |""".stripMargin)
        ),
        body = QuestionnaireTask(Seq(
          VideoQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt =
              """Working prototype video
                |""".stripMargin
          ),
          ShortTextQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt = "(Optional, not shown in critique) What was hardest about your work ...",
            hideInCrit = true
          )
        ))
      ))
      _ = println(s"Working video task has id ${workingVideo.id}")

    yield
      println("Done")

}
