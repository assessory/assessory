package com.assessory.clientpickle

import com.assessory.api.critique._
import com.assessory.api.due.DueDate
import com.assessory.api.question.{BooleanQuestion, ShortTextQuestion, Question, QuestionnaireTask}
import com.assessory.api.video.VideoTask
import com.assessory.api.{TaskDetails, Task}
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy.Ids._
import com.wbillingsley.handy.appbase._

import Pickles._
import com.assessory.api.client.invalidId
import org.scalatest.{Matchers, FlatSpec}

import scala.util.Success

class PickleSpec extends FlatSpec with Matchers {

  "Pickles" should "Pickle and unpickle Id" in  {
    val id = "myTest".asId[User]
    val pickled = write(id)
    val unpickled = read[Id[User,String]](pickled)

    unpickled should be (Success(id))
  }

  it should "Pickle and unpickle Question" in  {
    val made:Seq[Question] = Seq(
      ShortTextQuestion(id=invalidId, prompt="Hello world"),
      BooleanQuestion(id=invalidId, prompt="Hello world")
    )
    val pickled = write(made)
    val unpickled = read[Seq[Question]](pickled)

    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Course" in  {
    val made = Course(
      id = invalidId,
      addedBy = invalidId,
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
      target = invalidId,
      roles = CourseRole.roles,
      identity = IdentityLookup("foo", Some("bar"), Some("baz"))
    )
    val pickled = write(made)
    val unpickled = read[Course.PreenrolRow](pickled)

    unpickled should be (Success(made))
  }


  it should "Pickle and unpickle Course Preenrolment" in  {
    val made = new Course.Preenrol(
      id = invalidId
    )
    val pickled = write(made)
    val unpickled = read[Course.Preenrol](pickled)

    unpickled should be (Success(made))
  }



  it should "Pickle and unpickle GroupSet" in  {
    val made = GroupSet(
      id = invalidId,
      course = invalidId,
      name = Some("test group")
    )
    val pickled = write(made)
    val unpickled = read[GroupSet](pickled)
    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Group" in  {
    val made = Group(
      id = invalidId,
      set = invalidId,
      name = Some("test group"),
      members = Seq("1", "2", "3").asIds
    )
    val pickled = write(made)
    val unpickled = read[Group](pickled)
    unpickled should be (Success(made))
  }

  it should "Pickle and unpickle Task" in  {
    val made = Task(
      id = invalidId,
      course = invalidId,
      details = TaskDetails(
        name = Some("test crit"),
        description = Some("This is to test critiques can be sent between server and client"),
        published = DueDate(System.currentTimeMillis())
      ),
      body = CritiqueTask(
        strategy = AllocateStrategy(
          what = TTOutputs("invalid".asId),
          5
        ),
        task = QuestionnaireTask(
          questionnaire = Seq(
            ShortTextQuestion(id=invalidId, prompt="Hello world"),
            BooleanQuestion(id=invalidId, prompt="Hello world")
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
      id = invalidId,
      course = invalidId,
      details = TaskDetails(
        name = Some("test crit"),
        description = Some("This is to test critiques can be sent between server and client"),
        published = DueDate(System.currentTimeMillis())
      ),
      body = CritiqueTask(
        task = QuestionnaireTask(
          questionnaire = Seq(
            ShortTextQuestion(id=invalidId, prompt="Hello world"),
            BooleanQuestion(id=invalidId, prompt="Hello world")
          )
        ),
        strategy = AllocateStrategy(
          what = TTGroups(set=invalidId), number = 3
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
