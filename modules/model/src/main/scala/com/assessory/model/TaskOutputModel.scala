package com.assessory.model

import java.io.StringWriter
import au.com.bytecode.opencsv.CSVWriter
import com.assessory.api.{given, _}
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{Critique, CritiqueTask}
import com.assessory.api.question.{BooleanAnswer, BooleanQuestion, QuestionnaireTaskOutput, ShortTextAnswer, ShortTextQuestion, VideoAnswer}
import com.assessory.api.video.*
import com.assessory.asyncmongo.*
import com.assessory.api.wiring.Lookups.{given, _}
import com.wbillingsley.handy.{Approval, Id, Ref, RefFailed, RefMany, RefNone, RefOpt, lazily, refOps}
import com.assessory.api.appbase.{User, UserError}
import com.assessory.api.call.{Return, TaskOutputCall, ReturnTaskOutput, StandardReturn}
import com.assessory.api.question.FileAnswer

object TaskOutputModel {

  def withPerms(a:Approval[User], t:TaskOutput) = {
    for {
      edit <- a.askBoolean(Permissions.EditOutput(t.itself))
    } yield {
      WithPerms(
        Map(
          "edit" -> edit
        ),
        t)
    }
  }

  def get(a:Approval[User], t:Id[TaskOutput,String]) = {
    for {
      to <- t.lazily
      wp <- withPerms(a, to)
    } yield wp
  }

  /**
    * Calculates an appropriate "by" for this task and this user (might be the user, or might be the group they are in)
    *
    * @param task
    * @param u
    * @return
    */
  def byForTask(task:Task, u:User):Ref[By] = {
    if (task.details.individual || task.details.groupSet.isEmpty) {
      By.ByUser(u.id).itself
    } else {
      for {
        gsId <- task.details.groupSet.toRefOpt orFail new IllegalStateException(s"task ${task.id} is a group task with no groupset")
        gs <- gsId.lazily

        g <- GroupModel.myGroupInSet(u, gs) orFail UserError("You are not in a group but this is a group task")
      } yield By.ByGroup(g.id)
    }
  }


  def myOutputs(a:Approval[User], rTask:Ref[Task]) = {
    for {
      task <- rTask
      u <- a.who

      by <- byForTask(task, u)

      to <- TaskOutputDAO.byTaskAndBy(task.id, by)
    } yield to
  }

  /**
    * Retrieves all outputs for a given task
    */
  def allOutputs(a:Approval[User], rTask:Ref[Task]) = {
    for {
      task <- rTask
      u <- a.who
      approved <- a ask Permissions.EditTask(task.itself)
      to <- TaskOutputDAO.byTask(task.itself)
    } yield to
  }

  def create(a:Approval[User], task:Ref[Task], clientTaskOutput:TaskOutput, finalise:Boolean):Ref[WithPerms[TaskOutput]] = {
    for {
      t <- task
      approved <- a ask Permissions.ViewCourse(t.course.lazily)
      u <- a.who.require
      by <- byForTask(t, u)
      to = clientTaskOutput.copy(
        id=TaskOutputId(TaskOutputDAO.allocateId),
        task=t.id,
        by=by
      )
      saved <- TaskOutputDAO.saveSafe(to)
      finalised <- if (finalise) {
        // Finalise the task output
        TaskOutputDAO.finalise(saved)
      } else {
        // Don't finalise it; just return the saved item
        saved.itself
      }
      wp <- withPerms(a, finalised)
    } yield wp
  }

  def updateBody(a:Approval[User], clientTaskOutput:TaskOutput, finalise:Boolean) = {
    for {
      approved <- a ask Permissions.EditOutput(clientTaskOutput.id.lazily)
      saved <- TaskOutputDAO.updateBody(clientTaskOutput)
      finalised <- if (finalise) {
        // Finalise the task output
        TaskOutputDAO.finalise(saved)
      } else {
        // Don't finalise it; just return the saved item
        saved.itself
      }
      wp <- withPerms(a, finalised)
    } yield wp
  }

  def finalise(a:Approval[User], rTO:Ref[TaskOutput]):Ref[WithPerms[TaskOutput]] = {
    for {
      approved <- a ask Permissions.EditOutput(rTO)
      to <- rTO
      finalised <- TaskOutputDAO.finalise(to)
      wp <- withPerms(a, finalised)
    } yield wp
  }

  
  /**
    * Retrieves a Seq[VideoResource] from a TaskOutputBody
    */
  def getVideo(t:TaskOutputBody):Seq[VideoResource] = t match {
    case QuestionnaireTaskOutput(answers) => answers.collect({ case VideoAnswer(id, Some(vr)) => vr })
    case VideoTaskOutput(Some(vr)) => Seq(vr)
    case Critique(target, task) => getVideo(task)
    case _ => {
      println(s">> NO VIDEOS IN $t")
      Seq.empty
    }
  }

  def name(target:Target):Ref[String] = target match {
    case TargetUser(u) => for (user <- u.lazily) yield UserModel.displayName(user)
    case TargetGroup(g) => g.lazily.map(_.name.getOrElse("Unnamed group"))
    case TargetCourseReg(r) => r.lazily.flatMap(_.user.lazily).map(UserModel.displayName)
    case TargetTaskOutput(to) => for { o <- to.lazily; by <- name(o.by) } yield by
  }

  def name(by:By):Ref[String] = by match {
    case By.ByUser(u) => for (user <- u.lazily) yield UserModel.displayName(user)
    case By.ByGroup(g) => g.lazily.map(_.name.getOrElse("Unnamed group"))
  }

  def name(t:TaskOutput):Ref[String] = t.body match {
    case Critique(target, task) => for { targName <- name(target); byName <- name(t.by) } yield s"$byName on $targName"
    case _ => name(t.by)
  }

  def downloadVideoOutputs(approval:Approval[User], task:Ref[Task], path:String):RefMany[(Int, String)] = {

    import sys.process._

    def extractKalturaId(url:String):String = {
      val withWWW="(https\\:\\/\\/kaf.une.edu.au\\/media\\/[^#\\&\\?\\/]*\\/)([^#\\&\\?]*).*".r
      withWWW.findFirstMatchIn(url).map(_.group(2)).getOrElse(url)
    }

    val r:RefMany[(Int, String)] = for {
      o <- allOutputs(approval, task)
      name <- {
        println(s">> STARTING WORK ON OUTPUT ${o.id.id}")
        name(o)
      }
      (video, idx) <- getVideo(o.body).zipWithIndex.toRefMany
      url <- video match {
        case YouTube(url) => Some(url).toRefOpt
        case Kaltura(url) => {
          val id =  extractKalturaId(url)
          Some(s"https://cdnapisec.kaltura.com/p/424421/sp/42442100/embedIframeJs/uiconf_id/7033932/partner_id/424421?iframeembed=true&playerId=kaltura_player&entry_id=${id}").toRefOpt
        }
        case UnrecognisedVideoUrl(url) => Some(url).toRefOpt
        case _ => {
          println(s">> UNRECOGNISED VIDEO FOR ${o.id.id}")
          RefNone
        }
      }
    } yield {
      println(s">> OUTPUT ${o.id.id} has URL ${url}")

      val fname = s"$name $idx"
      val taskSeq = Seq("youtube-dl", "-o", s"""${path}/$fname""", url)

      println(s">> TASK IS $taskSeq")

      val result = taskSeq.!
      println(s">> DOWNLOAD RESULT for output ${o.id.id} $name was $result")
      result -> o.id.id
    }

    r
  }

  def handleCall(a:Approval[User], call:TaskOutputCall):Ref[Return] = call match {
    case TaskOutputCall.GetTaskOutput(id) =>
      for wp <- get(a, id) yield StandardReturn.ReturnWithPermissions(ReturnTaskOutput(wp.item), wp.perms)

    case TaskOutputCall.CreateTaskOutput(clientTO) =>
      for
        wp <- create(
          a = a,
          task = clientTO.task.lazily,
          clientTaskOutput = clientTO,
          finalise = false // TODO: allow finalising of tasks
        )
      yield StandardReturn.ReturnWithPermissions(ReturnTaskOutput(wp.item), wp.perms)

    case TaskOutputCall.MyOutputs(taskId) =>
      val rm = for t <- myOutputs(a, taskId.lazily) yield ReturnTaskOutput(t)
      for list <- rm.collect yield StandardReturn.ReturnMany(list)

    case TaskOutputCall.AllOutputs(taskId) =>
      val rm = for t <- allOutputs(a, taskId.lazily) yield ReturnTaskOutput(t)
      for list <- rm.collect yield StandardReturn.ReturnMany(list)

    case TaskOutputCall.UpdateBody(to) =>
      (for
        wp <- updateBody(a, to, false)
      yield StandardReturn.ReturnWithPermissions(ReturnTaskOutput(wp.item), wp.perms)) orElse StandardReturn.ReturnNone.itself

    case TaskOutputCall.Finalise(to) =>
      for wp <- finalise(a, to.itself) yield StandardReturn.ReturnWithPermissions(ReturnTaskOutput(wp.item), wp.perms)
  }


}
