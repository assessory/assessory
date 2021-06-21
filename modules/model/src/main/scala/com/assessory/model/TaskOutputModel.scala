package com.assessory.model

import java.io.StringWriter

import au.com.bytecode.opencsv.CSVWriter
import com.assessory.api.{given, _}
import com.assessory.api.client.WithPerms
import com.assessory.api.critique.{Critique, CritiqueTask}
import com.assessory.api.question.{VideoAnswer, BooleanQuestion, ShortTextQuestion, BooleanAnswer, ShortTextAnswer, QuestionnaireTaskOutput}
import com.assessory.api.video._
import com.assessory.asyncmongo._
import com.assessory.api.wiring.Lookups.{given, _}
import com.wbillingsley.handy.{Ref, RefFailed, RefOpt, RefNone, RefMany, refOps, Id, lazily, Approval}
import com.assessory.api.appbase.{UserError, User}

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
  def byForTask(task:Task, u:User):Ref[Target] = {
    if (task.details.individual || task.details.groupSet.isEmpty) {
      TargetUser(u.id).itself
    } else {
      for {
        gsId <- task.details.groupSet.toRefOpt orFail new IllegalStateException(s"task ${task.id} is a group task with no groupset")
        gs <- gsId.lazily

        g <- GroupModel.myGroupInSet(u, gs) orFail UserError("You are not in a group but this is a group task")
      } yield TargetGroup(g.id)
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

  def targetAsCsvString(a:Approval[User], t:Target):Ref[Seq[String]] = {

    def idNameFromUser(u:User):RefOpt[String] = {
      u.identities.find(_.service == I_STUDENT_NUMBER).flatMap(_.value)
        .orElse(u.identities.headOption.flatMap(_.username))
        .orElse(u.identities.headOption.flatMap(_.value))
        .toRefOpt
    }

    t match {
      case TargetUser(id) =>
        for {
          u <- a.cache.lookUp(id)
          id <- idNameFromUser(u) orFail new IllegalStateException(s"Failed to get ID name from user ${u.id.id}")
        } yield Seq(id, u.name.getOrElse(""))
      case TargetGroup(id) =>
        for {
          g <- a.cache.lookUp(id)
        } yield Seq(g.name.getOrElse(""))
      case TargetTaskOutput(id) =>
        for {
          to <- a.cache.lookUp(id)
          by <- targetAsCsvString(a, to.by)
        } yield by
      case _ => RefFailed(UserError("Can't represent this target as a string"))
    }
  }


  /**
   * Produces a CSV file of all the outputs for this task
    *
    * @param a
   * @param t
   * @return
   */
  def asCsv(a:Approval[User], t:Id[Task,String]) = {
    val sWriter = new StringWriter()
    val writer = new CSVWriter(sWriter)

    val rTask = t.lazily

    def outputs = for {
      task <- rTask
      approved <- a ask Permissions.EditTask(task.itself)
      output <- TaskOutputDAO.byTask(task.itself)
    } yield {
      println("FOUND OUTPUT " + output.id.id)
      output
    }


    // We don't write a header because we don't know how many columns the "for" or "by" lines should take up.

    def line(tob:TaskOutputBody):Ref[Seq[String]] = {

      println("CALLED FOR " + tob)

      tob match {
        case QuestionnaireTaskOutput(answers) => (answers map {
          case ShortTextAnswer(q, ans) => ans.getOrElse("")
          case BooleanAnswer(q, ans) => ans.map(_.toString).getOrElse("")
        }).itself
        case c: Critique => for {
          ofor <- targetAsCsvString(a, c.target)
          cols <- line(c.task)
        } yield ofor ++ cols
        case VideoTaskOutput(Some(YouTube(id))) => Seq(id).itself
        case _ => Seq("").itself
        //case _ => RefFailed(UserError(s"I don't know how to make a CSV for ${tob.kind}"))
      }
    }


    def write = (for {
      output <- outputs
      by <- targetAsCsvString(a, output.by)
      l <- line(output.body)
      line = by ++ l
    } yield {
      writer.writeNext(line.toArray)
      true
    }).collect

    for (written <- write) yield {
      writer.close()
      sWriter.toString
    }
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

}
