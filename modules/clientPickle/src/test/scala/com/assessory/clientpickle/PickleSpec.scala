package com.assessory.clientpickle

import com.assessory.api.critique._
import com.assessory.api.due.DueDate
import com.assessory.api.question._
import com.assessory.api.video.VideoTask
import com.assessory.api._
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import com.assessory.api.appbase._

import Pickles.{given, _}
import org.scalatest._
import flatspec._
import matchers._

import scala.util.Success

class PickleSpec extends AnyFlatSpec with should.Matchers {

  "Pickles" should "Pickle and unpickle Id" in  {
    val id = UserId("myTest")
    val pickled = write(id)
    val unpickled = read[UserId](pickled)

    unpickled should be (Success(id))
  }

  it should "Pickle and unpickle Question" in  {
    val made:Seq[Question] = Seq(
      ShortTextQuestion(id=QuestionId("invalid"), prompt="Hello world"),
      BooleanQuestion(id=QuestionId("invalid"), prompt="Hello world")
    )
    val pickled = write(made)
    val unpickled = read[Seq[Question]](pickled)

    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Course" in  {
    val made = Course(
      id = CourseId("invalid"),
      addedBy = RegistrationId("invalid"),
      title = Some("test course")
    )
    val pickled = write(made)
    val unpickled = read[Course](pickled)

    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Course Role" in {
    val made = CourseRole.staff
    val pickled = write(made)
    val unpickled = read[CourseRole](pickled)

    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Course Preenrolment Row" in  {
    val made = new Course.PreenrolRow(
      target = CourseId("invalid"),
      roles = CourseRole.roles,
      identity = IdentityLookup("foo", Some("bar"), Some("baz"))
    )
    val pickled = write(made)
    val unpickled = read[Course.PreenrolRow](pickled)

    unpickled should be (Success(made))
  }


  it should "Pickle and unpickle Course Preenrolment" in  {
    val made = new Course.Preenrol(
      id = PreenrolmentId("invalid")
    )
    val pickled = write(made)
    val unpickled = read[Course.Preenrol](pickled)

    unpickled should be (Success(made))
  }



  it should "Pickle and unpickle GroupSet" in  {
    val made = GroupSet(
      id = GroupSetId("invalid"),
      course = CourseId("invalid"),
      name = Some("test group")
    )
    val pickled = write(made)
    val unpickled = read[GroupSet](pickled)
    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Group" in  {
    val made = Group(
      id = GroupId("invalid"),
      set = GroupSetId("invalid"),
      name = Some("test group"),
      members = Seq("1", "2", "3").map(RegistrationId.apply)
    )
    val pickled = write(made)
    val unpickled = read[Group](pickled)
    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Task" in  {
    val made = Task(
      id = TaskId("invalid"),
      course = CourseId("invalid"),
      details = TaskDetails(
        name = Some("test crit"),
        description = Some("This is to test critiques can be sent between server and client"),
        published = DueDate(System.currentTimeMillis())
      ),
      body = CritiqueTask(
        strategy = AllocateStrategy(
          what = TTOutputs(TaskId("invalid")),
          5
        ),
        task = QuestionnaireTask(
          questionnaire = Seq(
            ShortTextQuestion(id=QuestionId("invalid"), prompt="Hello world"),
            BooleanQuestion(id=QuestionId("invalid"), prompt="Hello world")
          )
        )
      )
    )
    val pickled = write(made)
    val unpickled = read[Task](pickled)

    unpickled should be (Success(made))
  }


  it should "Pickle and unpickle Preallocated Crit Task" in  {
    val made = Task(
      id = TaskId("invalid"),
      course = CourseId("invalid"),
      details = TaskDetails(
        name = Some("test crit"),
        description = Some("This is to test critiques can be sent between server and client"),
        published = DueDate(System.currentTimeMillis())
      ),
      body = CritiqueTask(
        task = QuestionnaireTask(
          questionnaire = Seq(
            ShortTextQuestion(id=QuestionId("invalid"), prompt="Hello world"),
            BooleanQuestion(id=QuestionId("invalid"), prompt="Hello world")
          )
        ),
        strategy = AllocateStrategy(
          what = TTGroups(set=GroupSetId("invalid")), number = 3
        )
      )
    )
    val pickled = write(made)
    val unpickled = read[Task](pickled)

    unpickled should be (Success(made))
  }

  /*



it should "Pickle and unpickle Video Task" in  {
val made = Task(
  id = invalidId,
  course = invalidId,
  details = TaskDetails(
    name = Some("test crit"),
    description = Some("This is to test critiques can be sent between server and client"),
    published = DueDate(System.currentTimeMillis())
  ),
  body = VideoTask()
)
val pickled = write(made)
val unpickled = read[Task](pickled)

unpickled should be (made)
}

*/

}
