package com.assessory.asyncmongo.converters

import com.assessory.asyncmongo.{RegistrationDAO, UserDAO}
import com.wbillingsley.handy.appbase.{Course, User}
import com.wbillingsley.handy.Id._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

class UserBSpec extends FlatSpec with Matchers {

  "UserB" should "convert in both directions" in {
    val source = User(
      id = UserDAO.allocateId.asId,
      name = Some("Algernon")
    )
    UserB.read(UserB.write(source)) should be (Success(source))
  }

  "CourseB" should "convert in both directions" in {
    val source = Course(
      id = UserDAO.allocateId.asId,
      addedBy = RegistrationDAO.course.allocateId.asId,
      title = Some("Foo"),
      shortName = Some("Bar")
    )
    CourseB.read(CourseB.write(source)) should be (Success(source))
  }

}
