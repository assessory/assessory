package com.assessory.asyncmongo.converters

import com.assessory.asyncmongo.{RegistrationDAO, UserDAO}
import com.assessory.api.appbase.{Course, CourseId, RegistrationId, User, UserId}
import com.wbillingsley.handy.Id._
import org.scalatest._
import flatspec._
import matchers._

import scala.util.Success

class UserBSuite extends AnyFlatSpec with should.Matchers {

  "UserB" should "convert in both directions" in {
    val source = User(
      id = UserId(UserDAO.allocateId),
      name = Some("Algernon")
    )
    UserB.read(UserB.write(source)) should be (Success(source))
  }

  "CourseB" should "convert in both directions" in {
    val source = Course(
      id = CourseId(UserDAO.allocateId),
      addedBy = RegistrationId(RegistrationDAO.course.allocateId),
      title = Some("Foo"),
      shortName = Some("Bar")
    )
    CourseB.read(CourseB.write(source)) should be (Success(source))
  }

}
