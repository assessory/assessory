package org.assessory.play.cheatscript

import com.wbillingsley.handy.{Ref, RefFailed, RefItself, RefNone, RefOpt, RefOptFailed, RefSome, refOps}
import com.assessory.api.*
import call.*
import appbase.*
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{AllocateStrategy, Critique, CritiqueTask, TTOutputs, TargetMyStrategy}
import com.assessory.api.due.DueDate
import com.assessory.api.question.{BooleanQuestion, QuestionId, QuestionnaireTask, ShortTextQuestion, VideoQuestion}
import com.assessory.asyncmongo.TaskDAO
import com.assessory.clientpickle.CallClient

object Cosc220 {

  def createCourse()(using cc:CallClient):Ref[Course] =
    (for
      StandardReturn.ReturnWithPermissions(ReturnCourse(c), perms) <- cc.call(CourseCall.CreateCourse(Course(
        id=CourseId("invalid"),
        addedBy=RegistrationId("invalid"),
        title = Some("Software Development Studio 2"),
        shortName = Some("COSC220 2022"),
        shortDescription = Some("In which our heroes develop amazing software together..."),
        ltis = Seq(LTIConsumer("UNE moodle", "grumplestiltskin"))
      )))
    yield c) orFail IllegalStateException("Return from creating course was not what I expected")

  def getCourse()(using cc:CallClient):RefOpt[Course] = (for ReturnCourse(c) <- cc.call(CourseCall.ByShortName("COSC220 2022")) yield c)

  def ensureCourse()(using cc:CallClient) = getCourse() orElse createCourse()

  def createGroupSet(c:Course)(using cc:CallClient):Ref[GroupSet] =
    (for
      StandardReturn.ReturnWithPermissions(ReturnGroupSet(g), perms) <- cc.call(GroupSetCall.CreateGroupSet(GroupSet(
        id = GroupSetId("invalid"),
        course = c.id,
        name = Some("Project group"),
        parent = None
      )))
    yield g) orFail IllegalStateException("Return from creating gs was not what I expected")

  def getGroupSet(c:Course)(using cc:CallClient):RefOpt[GroupSet] =
    for ReturnGroupSet(g) <- cc.call(GroupSetCall.ByName(c.id, "Project group")) yield g

  def ensureGroupSet(c:Course)(using cc:CallClient):Ref[GroupSet] = getGroupSet(c) orElse createGroupSet(c)

  def createGroups(gs:GroupSet)(using cc:CallClient):Ref[Seq[Group]] = {
    /*val groups = Seq(
      "App Monsters", "The Programmers", "In Classers", "Bit by Bit", "Pure Logic",
      "Status 404", "CTRL+C", "CSTEM32", "The Dirty Bits", "The Front Enders", "Bug Smashers",
      "Coders", "Card Sharks", "DFENS", "Iterators", "The Colossal Titans", "The Global Gits",
      "KAPPS", "Software Chasers"
    )*/

    val groups = Seq("alt-f4")

    (for
      StandardReturn.ReturnMany(existingSeq) <- cc.call(GroupCall.GroupSetGroups(gs.id))
      existingGroups = for ReturnGroup(g) <- existingSeq yield g.name.getOrElse("")
      needed = groups.filterNot(n => existingGroups.contains(n))

      groupName <- needed.toRefMany
      ReturnGroup(group) <- cc.call(GroupCall.CreateGroup(Group(
        id = GroupId("invalid"),
        course = Some(gs.course),
        set = gs.id,
        name = Some(groupName),
        provenance = Some("api")
      )))
    yield group).collect

  }


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

      gs <- ensureGroupSet(course)
      _ = println(s"GroupSet has id ${gs.id}")

      groups <- createGroups(gs)
      _ = println(s"${groups.length} new groups created")

      progressVideo <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Progress Video"),
          open = DueDate(System.currentTimeMillis()),
          groupSet = Some(gs.id),
          individual = false,
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
          ),
          ShortTextQuestion(
            QuestionId(TaskDAO.allocateId),
            prompt =
              """(Hidden in critique). What's been the hardest part of the unit so far?
                |""".stripMargin,
            hideInCrit = true
          )
        ))
      ))
      _ = println(s"Video task has id ${progressVideo.id}")


      critique <- ensureTask(Task(
        id = TaskId("invalid"),
        course = course.id,
        details = TaskDetails(
          name = Some("Critique progress videos"),
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
