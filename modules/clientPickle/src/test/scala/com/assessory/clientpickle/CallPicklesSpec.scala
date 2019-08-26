package com.assessory.clientpickle

import com.assessory.api.call._
import com.wbillingsley.handy.appbase.{ActiveSession, Course, Group, GroupSet}
import org.scalatest.{FlatSpec, Matchers}
import com.wbillingsley.handy.Id._

import scala.util.Success

class CallPicklesSpec extends FlatSpec with Matchers {

  import CallPickles._

  "CallPickles" should "Pickle and unpickle GetSession" in {
    readCall(write(GetSession)) should be (Success(GetSession))
  }

  it should "Pickle and unpickle within WithSession" in {
    val a = ActiveSession("k", "ip")
    val w = WithSession(a, Register("e", "p", a))

    readCall(write(w)) should be (Success(w))
  }

  it should "Pickle and unpickle CreateCourse" in {
    val c = Course(
      id = "1234".asId,
      addedBy = "me".asId,
      title = Some("My great course")
    )

    readCall(write(CreateCourse(c))) should be (Success(CreateCourse(c)))
  }

  it should "Pickle and unpickle CreateGroupSetCSV" in {
    val setId="gs1".asId[GroupSet]
    val csv =
      """
        |student number,name,group name,parent name,social id
        |wb@example.com,W B,examples,studio 1,wb@github.com
        |""".stripMargin

    readCall(write(CreateGroupsFromCsv(setId, csv))) should be (Success(CreateGroupsFromCsv(setId, csv)))
  }


  it should "Pickle and unpickle ReturnGroupsData" in {
    val rgd= ReturnGroupsData(Seq(
      Group(id="1".asId, set="set1".asId, name=Some("g1")) -> Seq("a@example.com", "b@example.com"),
      Group(id="2".asId, set="set1".asId, name=Some("g2")) -> Seq("c@example.com", "d@example.com")
    ))

    val setId="gs1".asId[GroupSet]
    val csv =
      """
        |student number,name,group name,parent name,social id
        |wb@example.com,W B,examples,studio 1,wb@github.com
        |""".stripMargin

    readCall(write(CreateGroupsFromCsv(setId, csv))) should be (Success(CreateGroupsFromCsv(setId, csv)))
  }


}
