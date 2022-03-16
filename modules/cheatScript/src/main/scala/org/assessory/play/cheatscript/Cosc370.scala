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

/**
  * Created by wbilling on 21/02/2017.
  */
object Cosc370 {

  def createCourse()(using cc:CallClient):Ref[Course] =
    (for
      StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- cc.call(CourseCall.CreateCourse(Course(
        id=CourseId("invalid"),
        addedBy=RegistrationId("invalid"),
        title = Some("User Experience and Interaction Design"),
        shortName = Some("COSC370/570 2022"),
        shortDescription = Some("In which our heroes practice design thinking..."),
        ltis = Seq(LTIConsumer("UNE moodle", "grumplestiltskin"))
      )))
    yield c) orFail IllegalStateException("Return from creating course was not what I expected")

  def getCourse()(using cc:CallClient):RefOpt[Course] = (for ReturnCourse(c) <- cc.call(CourseCall.ByShortName("COSC370/570 2022")) yield c)

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

  def run()(using cc:CallClient) =
    for
      course <- ensureCourse()
      _ = println(s"Course has id ${course.id}")

      conceptVideo <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Concept Video"),
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
          open = DueDate(System.currentTimeMillis()),
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
          open = DueDate(System.currentTimeMillis()),
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
          open = DueDate(System.currentTimeMillis()),
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
          open = DueDate(System.currentTimeMillis()),
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
