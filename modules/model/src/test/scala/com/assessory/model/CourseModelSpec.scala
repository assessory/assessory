package com.assessory.model

import com.assessory.api.client.invalidId
import com.assessory.asyncmongo.DB
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.{ActiveSession, Course}
import org.specs2.mutable.Specification
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.{AfterEach, BeforeEach}


class CourseModelSpec(implicit ee: ExecutionEnv) extends Specification with BeforeEach with AfterEach {

  def before = {
    //DB.executionContext = scala.concurrent.ExecutionContext.global
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

  "CourseModel" should {

    "Allow users to create courses" in  {
      DB.dbName = "testAssessory"
      DoWiring.doWiring

      val myCoursesAfterSignup = for {
        u <- UserModel.signUp(Some("eg@example.com"), Some("password"), ActiveSession("1234", "127.0.0.1"))
        courseWithPerms <- CourseModel.create(Approval(RefSome(u)), Course(id=invalidId, addedBy=invalidId, title=Some("TestCourse")))
        course <- CourseModel.myCourses(Approval(RefSome(u)))
      } yield course.item.title

      myCoursesAfterSignup.collect.toFuture must beEqualTo(Seq(Some("TestCourse"))).await
    }


  }

}
