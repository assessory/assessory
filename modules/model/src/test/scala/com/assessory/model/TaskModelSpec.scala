package com.assessory.model

import com.assessory.api.critique.{CritiqueTask, TTGroups, TargetMyStrategy}
import com.assessory.api.wiring.Lookups
import com.assessory.api.{TargetGroup, Task, TaskDetails, TaskId}
import com.assessory.api.question.QuestionnaireTask
import com.assessory.asyncmongo.DB
import com.wbillingsley.handy.{Ref, RefSome, Approval, refOps, lazily}
import com.assessory.api.appbase.{ActiveSession, Course, CourseId, GroupSetId, RegistrationId}
import org.specs2.mutable.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.{AfterEach, BeforeEach}
import Lookups.{given, _}

class TaskModelSpec(implicit ee: ExecutionEnv) extends Specification with BeforeEach with AfterEach {

  def before = {
    scala.concurrent.blocking {
      DB.dbName = "testAssessory"
      DB.db.drop().head()
    }
  }

  def after = {
    scala.concurrent.blocking {
      DB.mongoClient.close()
    }
  }


  sequential

  "TaskModel" should {

    "Allow users to create tasks in courses" in  {
      DB.dbName = "testAssessory_courseModel"
      DoWiring.doWiring

      val saved = for {
        u <- UserModel.signUp(Some("eg@example.com"), Some("password"), ActiveSession("1234", "127.0.0.1"))
        courseWithPerms <- CourseModel.create(Approval(RefSome(u)), Course(id=CourseId("invalid"), addedBy=RegistrationId("invalid"), title=Some("TestCourse")))

        wp <- TaskModel.create(Approval(RefSome(u)), Task(
          id=TaskId("invalid"), course=courseWithPerms.item.id,
          details=TaskDetails(name=Some("Foo")),
          body=CritiqueTask(
            strategy=TargetMyStrategy(task=TaskId("123"), what=TTGroups(GroupSetId("123")), number=Some(3)),
            task=QuestionnaireTask(Seq.empty)
          )
        ))
        retrieved <- wp.item.id.lazily
      } yield retrieved.details.name

      saved.toFuture must beEqualTo(Some("Foo")).await
    }

    "Return the tasks in a course" in  {
      DB.dbName = "testAssessory_courseModel"
      DoWiring.doWiring

      val saved = for {
        u <- UserModel.signUp(Some("eg@example.com"), Some("password"), ActiveSession("1234", "127.0.0.1"))
        courseWithPerms <- CourseModel.create(Approval(RefSome(u)), Course(id=CourseId("invalid"), addedBy=RegistrationId("invalid"), title=Some("TestCourse")))

        task1 <- TaskModel.create(Approval(RefSome(u)), Task(
          id=TaskId("invalid"), course=courseWithPerms.item.id,
          details=TaskDetails(name=Some("Foo")),
          body=CritiqueTask(
            strategy=TargetMyStrategy(task=TaskId("123"), what=TTGroups(GroupSetId("123")), number=Some(3)),
            task=QuestionnaireTask(Seq.empty)
          )
        ))
        task2 <- TaskModel.create(Approval(RefSome(u)), Task(
          id=TaskId("invalid"), course=courseWithPerms.item.id,
          details=TaskDetails(name=Some("Bar")),
          body=CritiqueTask(
            strategy=TargetMyStrategy(task=TaskId("123"), what=TTGroups(GroupSetId("123")), number=Some(3)),
            task=QuestionnaireTask(Seq.empty)
          )
        ))
        wp <- TaskModel.courseTasks(Approval(RefSome(u)), courseWithPerms.item.itself)
      } yield wp.item.details.name

      saved.collect.toFuture must beEqualTo(Seq(Some("Foo"), Some("Bar"))).await
    }
  }

}
