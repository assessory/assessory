package com.assessory.model

import com.assessory.api.critique.{TargetMyStrategy, TTGroups, CritiqueTask}
import com.assessory.api.wiring.Lookups
import com.assessory.api.{TaskDetails, Task, TargetGroup}
import com.assessory.api.question.QuestionnaireTask
import com.assessory.api.client.invalidId
import com.assessory.asyncmongo.DB

import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{ActiveSession, Course}
import org.specs2.mutable.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.{AfterEach, BeforeEach}

import Ref._
import Id._
import Lookups._

class TaskModelSpec(implicit ee: ExecutionEnv) extends Specification with BeforeEach with AfterEach {

  def before = {
    scala.concurrent.blocking {
      DB.dbName = "testAssessory"
      DB.db.drop().toFuture()
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
        courseWithPerms <- CourseModel.create(Approval(RefSome(u)), Course(id=invalidId, addedBy=invalidId, title=Some("TestCourse")))

        wp <- TaskModel.create(Approval(RefSome(u)), Task(
          id=invalidId, course=courseWithPerms.item.id,
          details=TaskDetails(name=Some("Foo")),
          body=CritiqueTask(
            strategy=TargetMyStrategy(task="123".asId, what=TTGroups("123".asId), number=Some(3)),
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
        courseWithPerms <- CourseModel.create(Approval(RefSome(u)), Course(id=invalidId, addedBy=invalidId, title=Some("TestCourse")))

        task1 <- TaskModel.create(Approval(RefSome(u)), Task(
          id=invalidId, course=courseWithPerms.item.id,
          details=TaskDetails(name=Some("Foo")),
          body=CritiqueTask(
            strategy=TargetMyStrategy(task="123".asId, what=TTGroups("123".asId), number=Some(3)),
            task=QuestionnaireTask(Seq.empty)
          )
        ))
        task2 <- TaskModel.create(Approval(RefSome(u)), Task(
          id=invalidId, course=courseWithPerms.item.id,
          details=TaskDetails(name=Some("Bar")),
          body=CritiqueTask(
            strategy=TargetMyStrategy(task="123".asId, what=TTGroups("123".asId), number=Some(3)),
            task=QuestionnaireTask(Seq.empty)
          )
        ))
        wp <- TaskModel.courseTasks(Approval(RefSome(u)), courseWithPerms.item.itself)
      } yield wp.item.details.name

      saved.collect.toFuture must beEqualTo(Seq(Some("Foo"), Some("Bar"))).await
    }
  }

}
