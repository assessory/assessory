package com.assessory.clientpickle

import io.circe._
import io.circe
import io.circe.syntax._
import io.circe.parser.decode

import scala.util.Try
import com.assessory.api.critique._
import com.assessory.api._
import com.assessory.api.client.{EmailAndPassword, WithPerms}
import com.assessory.api.due.{Due, DueDate, DuePerGroup, NoDue}
import com.assessory.api.video._
import question._
import com.assessory.api.appbase._
import com.wbillingsley.handy.{HasKind, EmptyKind, Id, Ids, Ref}

import scala.concurrent.Future

object Pickles {

  private def stringIdDecoder[T](constr:String => T):Decoder[T] = (c: HCursor) => {
    c.downField("id").as[String].map(constr)
  }
  given userIdDecoder:Decoder[UserId] = stringIdDecoder(UserId.apply)
  given courseIdDecoder:Decoder[CourseId] = stringIdDecoder(CourseId.apply)
  given taskIdDecoder:Decoder[TaskId] = stringIdDecoder(TaskId.apply)
  given taskOutputIdDecoder:Decoder[TaskOutputId] = stringIdDecoder(TaskOutputId.apply)
  given groupIdDecoder:Decoder[GroupId] = stringIdDecoder(GroupId.apply)
  given groupSetIdDecoder:Decoder[GroupSetId] = stringIdDecoder(GroupSetId.apply)
  given questionIdDecoder:Decoder[QuestionId] = stringIdDecoder(QuestionId.apply)
  given critAllocationIdDecoder:Decoder[CritAllocationId] = stringIdDecoder(CritAllocationId.apply)
  given smallFileIdDecoder:Decoder[SmallFileId] = stringIdDecoder(SmallFileId.apply)
  given registrationIdDecoder[T, R, K <: HasKind]:Decoder[RegistrationId[T, R, K]] = stringIdDecoder(RegistrationId.apply[T, R, K])
  given [W, T, R, RT]:Decoder[PreenrolmentId[W, T, R, RT]] = stringIdDecoder(PreenrolmentId[W, T, R, RT])

  given stringIdEncoder[TT, T <: Id[TT, String]]: Encoder[T] = (id: T) => Json.obj("id" -> Json.fromString(id.id))

  given idKeyEncoder[TT, T <: Id[TT, String]]: KeyEncoder[T] = (id: T) => id.id

  private def idKeyDecoder[TT, T <: Id[TT, String]](cons: String => T):KeyDecoder[T] = (id:String) => Some(cons(id))
  given KeyDecoder[Id[Group, String]] = idKeyDecoder(GroupId.apply)

  val shortTextQuestionEncoder: Encoder[ShortTextQuestion] = (q:ShortTextQuestion) => Json.obj(
    "kind" -> "shortText".asJson,
    "id" -> q.id.asJson,
    "prompt" -> q.prompt.asJson,
    "hideInCrit" -> q.hideInCrit.asJson,
    "maxLength" -> q.maxLength.asJson
  )

  val shortTextQuestionDecoder: Decoder[ShortTextQuestion] = (c:HCursor) => for {
    id <- c.downField("id").as[QuestionId]
    prompt <- c.downField("prompt").as[String]
    hideInCrit <- c.downField("hideInCrit").as[Boolean]
    maxLength <- c.downField("maxLength").as[Option[Int]]
  } yield ShortTextQuestion(id, prompt, maxLength, hideInCrit)

  val booleanQuestionEncoder:Encoder[BooleanQuestion] = (q:BooleanQuestion) => Json.obj(
    "kind" -> "boolean".asJson,
    "id" -> q.id.asJson,
    "prompt" -> q.prompt.asJson,
    "hideInCrit" -> q.hideInCrit.asJson
  )

  val booleanQuestionDecoder: Decoder[BooleanQuestion] = (c:HCursor) => for {
    id <- c.downField("id").as[QuestionId]
    prompt <- c.downField("prompt").as[String]
    hideInCrit <- c.downField("hideInCrit").as[Boolean]
  } yield BooleanQuestion(id, prompt, hideInCrit)

  val videoQuestionEncoder:Encoder[VideoQuestion] = (q:VideoQuestion) => Json.obj(
    "kind" -> "video".asJson,
    "id" -> q.id.asJson,
    "prompt" -> q.prompt.asJson,
    "hideInCrit" -> q.hideInCrit.asJson
  )

  val videoQuestionDecoder: Decoder[VideoQuestion] = (c:HCursor) => for {
    id <- c.downField("id").as[QuestionId]
    prompt <- c.downField("prompt").as[String]
    hideInCrit <- c.downField("hideInCrit").as[Boolean]
  } yield VideoQuestion(id, prompt, hideInCrit)

  implicit val questionEncoder: Encoder[Question] = {
    case s: ShortTextQuestion => shortTextQuestionEncoder(s)
    case b: BooleanQuestion => booleanQuestionEncoder(b)
    case b: VideoQuestion => videoQuestionEncoder(b)
  }
  implicit val questionDecoder: Decoder[Question] = (c:HCursor) => c.downField("kind").as[String] flatMap {
    case "shortText" => shortTextQuestionDecoder(c)
    case "boolean" => booleanQuestionDecoder(c)
    case "video" => videoQuestionDecoder(c)
  }

  implicit val courseRoleEncoder: Encoder[CourseRole] = (r:CourseRole) => Json.obj("role" -> r.r.asJson)
  implicit val courseRoleDecoder: Decoder[CourseRole] = (c:HCursor) => c.downField("role").as[String].map(CourseRole.apply)

  implicit val ltiConsumerEncoder: Encoder[LTIConsumer] = (lti:LTIConsumer) => Json.obj(
    "clientKey" -> lti.clientKey.asJson, "comment" -> lti.comment.asJson, "secret" -> lti.secret.asJson
  )
  implicit val ltiConsumerDecoder: Decoder[LTIConsumer] =(c:HCursor) => for {
    clientKey <- c.downField("clientKey").as[String]
    comment <- c.downField("comment").as[Option[String]].map(_.getOrElse("")) // FIXME: this is null in the database for some courses
    secret <- c.downField("secret").as[Option[String]]
  } yield LTIConsumer(clientKey, comment, secret)

  implicit val courseEncoder: Encoder[Course] = (c:Course) => Json.obj(
    "id" -> c.id.asJson, "addedBy" -> c.addedBy.asJson, "coverImage" -> c.coverImage.asJson,
    "shortName" -> c.shortName.asJson, "shortDescription" -> c.shortDescription.asJson, "title" -> c.title.asJson,
    "created" -> c.created.asJson, "ltis" -> c.ltis.asJson, "secret" -> c.secret.asJson, "website" -> c.website.asJson
  )
  implicit val courseDecoder: Decoder[Course] = (c:HCursor) => for {
    id <- c.downField("id").as[CourseId]
    addedBy <- c.downField("addedBy").as[RegistrationId[Course, CourseRole, HasKind]]
    coverImage <- c.downField("coverImage").as[Option[String]]
    shortName <- c.downField("shortName").as[Option[String]]
    shortDesc <- c.downField("shortDescription").as[Option[String]]
    title <- c.downField("title").as[Option[String]]
    created <- c.downField("created").as[Long]
    ltis <- c.downField("ltis").as[Seq[LTIConsumer]]
    secret <- c.downField("secret").as[String]
    website <- c.downField("website").as[Option[String]]
  } yield Course(
    id=id, addedBy=addedBy, coverImage=coverImage, shortName=shortName, shortDescription=shortDesc, title=title,
    created=created, ltis=ltis, secret=secret, website=website
  )

  implicit val identityEncoder: Encoder[Identity] = (i:Identity) => Json.obj(
    "service" -> i.service.asJson, "value" -> i.value.asJson, "avatar" -> i.avatar.asJson,
    "username" -> i.username.asJson, "since" -> i.since.asJson
  )
  implicit val identityDecoder: Decoder[Identity] = (c:HCursor) => for {
    service <- c.downField("service").as[String]
    value <- c.downField("value").as[Option[String]]
    avatar <- c.downField("avatar").as[Option[String]]
    username <- c.downField("username").as[Option[String]]
    since <- c.downField("since").as[Long]
  } yield Identity(service=service, value=value, avatar=avatar, username=username, since=since)

  implicit val courseRegEncoder: Encoder[Course.Reg] = (r:Course.Reg) => Json.obj(
    "id" -> r.id.asJson,
    "user" -> r.user.asJson,
    "target" -> r.target.asJson,
    "roles" -> r.roles.asJson,
    "updated" -> r.updated.asJson,
    "created" -> r.created.asJson
  )

  implicit val courseRegDecoder: Decoder[Course.Reg] = (c: HCursor) => {
    for {
      id <- c.downField("id").as[RegistrationId[Course, CourseRole, HasKind]]
      user <- c.downField("user").as[UserId]
      target <- c.downField("target").as[CourseId]
      roles <- c.downField("roles").as[Set[CourseRole]]
      updated <- c.downField("updated").as[Long]
      created <- c.downField("created").as[Long]
    } yield {
      new Course.Reg(id=id, user=user, target=target, roles=roles, updated=updated, created=created, provenance=EmptyKind)
    }
  }

  implicit val groupRoleEnc: Encoder[GroupRole] = (g:GroupRole) => Json.obj("role" -> g.r.asJson)
  implicit val groupRoleDec: Decoder[GroupRole] = (c:HCursor) => c.downField("role").as[String].map(GroupRole.apply)

  implicit val groupRegEncoder: Encoder[Group.Reg] = (r:Group.Reg) => Json.obj(
    "id" -> r.id.asJson,
    "user" -> r.user.asJson,
    "target" -> r.target.asJson,
    "roles" -> r.roles.asJson,
    "updated" -> r.updated.asJson,
    "created" -> r.created.asJson
  )

  implicit val groupRegDecoder: Decoder[Group.Reg] = (c: HCursor) => {
    for {
      id <- c.downField("id").as[RegistrationId[Group, GroupRole, HasKind]]
      user <- c.downField("user").as[UserId]
      target <- c.downField("target").as[GroupId]
      roles <- c.downField("roles").as[Set[GroupRole]]
      updated <- c.downField("updated").as[Long]
      created <- c.downField("created").as[Long]
    } yield {
      new Group.Reg(id=id, user=user, target=target, roles=roles, updated=updated, created=created, provenance=EmptyKind)
    }
  }

  implicit val identityLookupEncoder: Encoder[IdentityLookup] = (i:IdentityLookup) => Json.obj(
    "service" -> i.service.asJson, "username" -> i.username.asJson, "value" -> i.value.asJson
  )
  implicit val identityLookupDecoder: Decoder[IdentityLookup] = (c:HCursor) => for {
    service <- c.downField("service").as[String]
    username <- c.downField("username").as[Option[String]]
    value <- c.downField("value").as[Option[String]]
  } yield IdentityLookup(service, value, username)

  implicit def usedEncoder[T]: Encoder[Used[T]] = (u:Used[T]) => Json.obj(
    "target" -> u.target.asJson, "time" -> u.time.asJson
  )

  implicit def usedDecoder[T](using idConstr: String => Id[T, String]): Decoder[Used[T]] = (c: HCursor) => {
    for {
      target <- c.downField("target").downField("id").as[String].map(idConstr)
      time <- c.downField("time").as[Long]
    } yield Used(target, time)
  }

  implicit val coursePreenrolRowEncoder: Encoder[Course.PreenrolRow] = (r:Course.PreenrolRow) => Json.obj(
    "target" -> r.target.asJson,
    "identity" -> r.identity.asJson,
    "roles" -> r.roles.asJson,
    "used" -> r.used.asJson
  )

  implicit val coursePreenrolRowDecoder: Decoder[Course.PreenrolRow] = (c:HCursor) => {
    given Decoder[Used[Course.Reg]] = usedDecoder(using RegistrationId[Course, CourseRole, HasKind].apply)

    for {
      target <- c.downField("target").as[CourseId]
      identity <- c.downField("identity").as[IdentityLookup]
      roles <- c.downField("roles").as[Set[CourseRole]]
      used <- c.downField("used").as[Option[Used[Course.Reg]]]
    } yield Preenrolment.Row[Course, CourseRole, Course.Reg](target=target, identity=identity, roles=roles, used=used)
  }

  implicit val coursePreenrolEncoder: Encoder[Course.Preenrol] = (p:Course.Preenrol) => Json.obj(
    "id" -> p.id.asJson, "name" -> p.name.asJson, "rows" -> p.rows.asJson, "within" -> p.within.asJson,
    "created" -> p.created.asJson, "modified" -> p.modified.asJson
  )
  implicit val coursePreenrolDecoder: Decoder[Course.Preenrol] = (c:HCursor) => for {
    id <- c.downField("id").as[PreenrolmentId[Course, Course, CourseRole, Course.Reg]]
    name <- c.downField("name").as[Option[String]]
    rows <- c.downField("rows").as[Seq[Course.PreenrolRow]]
    within <- c.downField("within").as[Option[CourseId]]
    created <- c.downField("created").as[Long]
    modified <- c.downField("modified").as[Long]
  } yield Preenrolment(id=id, name=name, rows=rows, within=within, created=created, modified=modified)

  implicit val groupSetEncoder: Encoder[GroupSet] = (gs:GroupSet) => Json.obj(
    "id" -> gs.id.asJson, "name" -> gs.name.asJson, "parent" -> gs.parent.asJson, "course" -> gs.course.asJson,
    "description" -> gs.description.asJson, "created" -> gs.created.asJson
  )
  implicit val groupSetDecoder: Decoder[GroupSet] = (c:HCursor) => for {
    id <- c.downField("id").as[GroupSetId]
    name <- c.downField("name").as[Option[String]]
    parent <- c.downField("parent").as[Option[GroupSetId]]
    course <- c.downField("course").as[CourseId]
    desc <- c.downField("description").as[Option[String]]
    created <- c.downField("created").as[Long]
  } yield GroupSet(id=id, name=name, parent=parent, course=course, description=desc, created=created)

  implicit val groupEncoder: Encoder[Group] = (g:Group) => Json.obj(
    "id" -> g.id.asJson, "name" -> g.name.asJson, "set" -> g.set.asJson, "course" -> g.course.asJson,
    "parent" -> g.parent.asJson, "members" -> g.members.asJson, "provenance" -> g.provenance.asJson,
    "created" -> g.created.asJson
  )
  implicit val groupDecoder: Decoder[Group] = (c:HCursor) => for {
    id <- c.downField("id").as[GroupId]
    name <- c.downField("name").as[Option[String]]
    set <- c.downField("set").as[GroupSetId]
    course <- c.downField("course").as[Option[CourseId]]
    parent <- c.downField("parent").as[Option[GroupId]]
    members <- c.downField("members").as[Seq[RegistrationId[Group, GroupRole, HasKind]]]
    provenance <- c.downField("provenance").as[Option[String]]
    created <- c.downField("created").as[Long]
  } yield Group(
    id=id, name=name, set=set, course=course, parent=parent, members=members, provenance=provenance,
    created=created
  )


  implicit val questionnaireTaskEncoder: Encoder[QuestionnaireTask] = (q:QuestionnaireTask) => Json.obj(
    "kind" -> "questionnaire".asJson, "questionnaire" -> q.questionnaire.asJson
  )
  implicit val questionnaireTaskDecoder: Decoder[QuestionnaireTask] = (c:HCursor) => for {
    questionnaire <- c.downField("questionnaire").as[Seq[Question]]
  } yield QuestionnaireTask(questionnaire)

  val TTSelfEncoder: Encoder[TTSelf.type] = (_) => Json.obj("kind" -> "self".asJson)
  val TTSelfDecoder: Decoder[TTSelf.type] = (c:HCursor) => c.downField("kind").as[String].map(_ => TTSelf)
  val TTGroupsEncoder: Encoder[TTGroups] = (t:TTGroups) => Json.obj("kind" -> "groups".asJson, "set" -> t.set.asJson)
  val TTGroupsDecoder: Decoder[TTGroups] = (c:HCursor) => c.downField("set").as[GroupSetId].map(TTGroups.apply)
  val TTOutputsEncoder: Encoder[TTOutputs] = (t:TTOutputs) => Json.obj("kind" -> "outputs".asJson, "task" -> t.task.asJson)
  val TTOutputsDecoder: Decoder[TTOutputs] = (c:HCursor) => c.downField("task").as[TaskId].map(TTOutputs.apply)

  implicit val targetTypeEncoder: Encoder[TargetType] = {
    case TTSelf => TTSelfEncoder(TTSelf)
    case t:TTGroups => TTGroupsEncoder(t)
    case t:TTOutputs => TTOutputsEncoder(t)
  }
  implicit val targetTypeDecoder: Decoder[TargetType] = (c:HCursor) => c.downField("kind").as[String].flatMap {
    case "self" => TTSelfDecoder(c)
    case "groups" => TTGroupsDecoder(c)
    case "outputs" => TTOutputsDecoder(c)
  }

  val targetMyStrategyEncoder: Encoder[TargetMyStrategy] = (t:TargetMyStrategy) => Json.obj(
    "kind" -> "my".asJson, "task" -> t.task.asJson, "what" -> t.what.asJson ,"number" -> t.number.asJson
  )
  val targetMyStrategyDecoder: Decoder[TargetMyStrategy] = (c:HCursor) => for {
    task <- c.downField("task").as[TaskId]
    what <- c.downField("what").as[TargetType]
    number <- c.downField("number").as[Option[Int]]
  } yield TargetMyStrategy(task=task, what=what, number=number)
  val allocateStrategyEncoder: Encoder[AllocateStrategy] = (t:AllocateStrategy) => Json.obj(
    "kind" -> "allocate".asJson, "what" -> t.what.asJson ,"number" -> t.number.asJson
  )
  val allocateStrategyDecoder: Decoder[AllocateStrategy] = (c:HCursor) => for {
    what <- c.downField("what").as[TargetType]
    number <- c.downField("number").as[Int]
  } yield AllocateStrategy(what=what, number=number)
  val anyStrategyEncoder: Encoder[AnyStrategy] = (t:AnyStrategy) => Json.obj(
    "kind" -> "any".asJson, "what" -> t.what.asJson ,"number" -> t.number.asJson
  )
  val anyStrategyDecoder: Decoder[AnyStrategy] = (c:HCursor) => for {
    what <- c.downField("what").as[TargetType]
    number <- c.downField("number").as[Int]
  } yield AnyStrategy(what=what, number=number)

  implicit val critiqueTargetStrategyEncoder: Encoder[CritTargetStrategy] = {
    case t:TargetMyStrategy => targetMyStrategyEncoder(t)
    case t:AllocateStrategy => allocateStrategyEncoder(t)
    case t:AnyStrategy => anyStrategyEncoder(t)
  }
  implicit val critiqueTargetStrategyDecoder: Decoder[CritTargetStrategy] = (c:HCursor) => c.downField("kind").as[String] flatMap {
    case "my" => targetMyStrategyDecoder(c)
    case "allocate" => allocateStrategyDecoder(c)
    case "any" => anyStrategyDecoder(c)
  }

  implicit val critiqueTaskEncoder: Encoder[CritiqueTask] = (c:CritiqueTask) => Json.obj(
    "kind" -> "critique".asJson, "strategy" -> c.strategy.asJson, "task" -> c.task.asJson
  )
  implicit val critiqueTaskDecoder: Decoder[CritiqueTask] = (c:HCursor) => for {
    strategy <- c.downField("strategy").as[CritTargetStrategy]
    task <- c.downField("task").as[TaskBody]
  } yield CritiqueTask(strategy=strategy, task=task)

  val dueDateEncoder: Encoder[DueDate] = (d:DueDate) => Json.obj(
    "kind" -> "date".asJson, "time" -> d.time.asJson
  )
  val dueDateDecoder: Decoder[DueDate] = (c:HCursor) => for {
    time <- c.downField("time").as[Long]
  } yield DueDate(time)
  val duePerGroupEncoder: Encoder[DuePerGroup] = (d:DuePerGroup) => Json.obj(
    "kind" -> "perGroup".asJson, "times" -> d.times.asJson
  )
  val duePerGroupDecoder: Decoder[DuePerGroup] = (c:HCursor) => for {
    times <- c.downField("times").as[Map[Id[Group, String], Long]]
  } yield DuePerGroup(times)

  implicit val dueEncoder: Encoder[Due] = {
    case d:DueDate => dueDateEncoder(d)
    case d:DuePerGroup => duePerGroupEncoder(d)
    case NoDue => Json.obj("kind" -> "No Due".asJson)
  }
  implicit val dueDecoder: Decoder[Due] = (c:HCursor) => c.downField("kind").as[String] flatMap {
    case "date" => dueDateDecoder(c)
    case "perGroup" => duePerGroupDecoder(c)
    case "No Due" => Right(NoDue)
  }

  val mustHaveFinishedEncoder: Encoder[MustHaveFinished] = (m:MustHaveFinished) => Json.obj(
    "kind" -> "Must have finished".asJson, "task" -> m.task.asJson
  )
  val mustHaveFinishedDecoder: Decoder[MustHaveFinished] = (c:HCursor) => c.downField("task").as[TaskId].map(MustHaveFinished.apply)

  implicit val taskRulesEncoder: Encoder[TaskRule] = {
    case m:MustHaveFinished => mustHaveFinishedEncoder(m)
  }
  implicit val taskRulesDecoder: Decoder[TaskRule] = (c:HCursor) => c.downField("kind").as[String] flatMap {
    case "Must have finished" => mustHaveFinishedDecoder(c)
  }

  implicit val taskDetailsEncoder: Encoder[TaskDetails] = (t:TaskDetails) => Json.obj(
    "name" -> t.name.asJson, "description" -> t.description.asJson, "created" -> t.created.asJson,
    "individual" -> t.individual.asJson, "groupSet" -> t.groupSet.asJson, "restrictions" -> t.restrictions.asJson,
    "open" -> t.open.asJson, "due" -> t.due.asJson, "closed" -> t.closed.asJson, "published" -> t.published.asJson
  )
  implicit val taskDetailsDecoder: Decoder[TaskDetails] = (c:HCursor) => for {
    name <- c.downField("name").as[Option[String]]
    desc <- c.downField("description").as[Option[String]]
    created <- c.downField("created").as[Long]
    individual <- c.downField("individual").as[Boolean]
    groupSet <- c.downField("groupSet").as[Option[GroupSetId]]
    restrictions <- c.downField("restrictions").as[Seq[TaskRule]]
    open <- c.downField("open").as[Due]
    due <- c.downField("due").as[Due]
    closed <- c.downField("closed").as[Due]
    published <- c.downField("published").as[Due]
  } yield TaskDetails(
    name=name, description=desc, created=created, individual=individual, groupSet=groupSet, restrictions=restrictions,
    open=open, due=due, closed=closed, published=published
  )

  implicit val taskBodyEncoder: Encoder[TaskBody] = {
    case qt:QuestionnaireTask => questionnaireTaskEncoder(qt)
    case ct:CritiqueTask => critiqueTaskEncoder(ct)
    case EmptyTaskBody => Json.obj("kind" -> Json.fromString("empty"))
  }

  implicit val taskBodyDecoder: Decoder[TaskBody] = (c: HCursor) => {
    c.downField("kind").as[String].flatMap {
      case "questionnaire" => questionnaireTaskDecoder(c)
      case "critique" => critiqueTaskDecoder(c)
      case "empty" => Right(EmptyTaskBody)
    }
  }

  implicit val taskEncoder: Encoder[Task] = (t:Task) => Json.obj(
    "id" -> t.id.asJson,
    "course" -> t.course.asJson,
    "details" -> t.details.asJson,
    "body" -> t.body.asJson
  )
  implicit val taskDecoder: Decoder[Task] = (c:HCursor) => for {
    id <- c.downField("id").as[TaskId]
    course <- c.downField("course").as[CourseId]
    details <- c.downField("details").as[TaskDetails]
    body <- c.downField("body").as[TaskBody]
  } yield Task(id=id, course=course, details=details, body=body)

  val targetUserEncoder: Encoder[TargetUser] = (t:TargetUser) => Json.obj("kind" -> "user".asJson, "id" -> t.id.asJson)
  val targetUserDecoder: Decoder[TargetUser] = (c:HCursor) => c.downField("id").as[UserId].map(TargetUser.apply)
  val targetCourseRegEncoder: Encoder[TargetCourseReg] = (t:TargetCourseReg) => Json.obj("kind" -> "courseReg".asJson, "id" -> t.id.asJson)
  val targetCourseRegDecoder: Decoder[TargetCourseReg] = (c:HCursor) => c.downField("id").as[RegistrationId[Course, CourseRole, HasKind]].map(TargetCourseReg.apply)
  val targetGroupEncoder: Encoder[TargetGroup] = (t:TargetGroup) => Json.obj("kind" -> "group".asJson, "id" -> t.id.asJson)
  val targetGroupDecoder: Decoder[TargetGroup] = (c:HCursor) => c.downField("id").as[GroupId].map(TargetGroup.apply)
  val targetTaskOutputEncoder: Encoder[TargetTaskOutput] = (t:TargetTaskOutput) => Json.obj("kind" -> "output".asJson, "id" -> t.id.asJson)
  val targetTaskOutputDecoder: Decoder[TargetTaskOutput] = (c:HCursor) => c.downField("id").as[TaskOutputId].map(TargetTaskOutput.apply)

  implicit val targetEncoder: Encoder[Target] = {
    case t:TargetUser => targetUserEncoder(t)
    case t:TargetCourseReg => targetCourseRegEncoder(t)
    case t:TargetGroup => targetGroupEncoder(t)
    case t:TargetTaskOutput => targetTaskOutputEncoder(t)
  }
  implicit val targetDecoder: Decoder[Target] = (c:HCursor) => c.downField("kind").as[String].flatMap {
    case "user" => targetUserDecoder(c)
    case "courseReg" => targetCourseRegDecoder(c)
    case "group" => targetGroupDecoder(c)
    case "output" => targetTaskOutputDecoder(c)
  }

  implicit val critiqueEncoder: Encoder[Critique] = (c:Critique) => Json.obj(
    "kind" -> "critique".asJson, "target" -> c.target.asJson, "task" -> c.task.asJson
  )
  implicit val critiqueDecoder: Decoder[Critique] = (c:HCursor) => for {
    target <- c.downField("target").as[Target]
    task <- c.downField("task").as[TaskOutputBody]
  } yield Critique(target=target, task=task)

  val youTubeEncoder: Encoder[YouTube] = (v:YouTube) => Json.obj("kind" -> "youtube".asJson, "id" -> v.ytId.asJson)
  val youtubeDecoder: Decoder[YouTube] = (c:HCursor) => c.downField("id").as[String].map(YouTube.apply)
  val kalturaEncoder: Encoder[Kaltura] = (v:Kaltura) => Json.obj("kind" -> "kaltura".asJson, "id" -> v.kId.asJson)
  val kalturaDecoder: Decoder[Kaltura] = (c:HCursor) => c.downField("id").as[String].map(Kaltura.apply)
  val unrecognisedVideoEncoder: Encoder[UnrecognisedVideoUrl] = (v:UnrecognisedVideoUrl) => Json.obj("kind" -> "unrecognised".asJson, "url" -> v.url.asJson)
  val unrecognisedVideoDecoder: Decoder[UnrecognisedVideoUrl] = (c:HCursor) => c.downField("url").as[String].map(UnrecognisedVideoUrl.apply)

  implicit val videoResourceEncoder: Encoder[VideoResource] = {
    case v:YouTube => youTubeEncoder(v)
    case v:Kaltura => kalturaEncoder(v)
    case v:UnrecognisedVideoUrl => unrecognisedVideoEncoder(v)
  }
  implicit val videoResourceDecoder: Decoder[VideoResource] = (c:HCursor) => c.downField("kind").as[String].flatMap {
    case "youtube" => youtubeDecoder(c)
    case "kaltura" => kalturaDecoder(c)
    case "unrecognised" => unrecognisedVideoDecoder(c)
  }

  val shortTextAnswerEncoder: Encoder[ShortTextAnswer] = (a:ShortTextAnswer) => Json.obj("kind" -> "shortText".asJson, "question" -> a.question.asJson, "answer" -> a.answer.asJson)
  val shortTextAnswerDecoder: Decoder[ShortTextAnswer] = (c:HCursor) => for {
    question <- c.downField("question").as[QuestionId]
    answer <- c.downField("answer").as[Option[String]]
  } yield ShortTextAnswer(question=question, answer=answer)
  val booleanAnswerEncoder: Encoder[BooleanAnswer] = (a:BooleanAnswer) => Json.obj("kind" -> "boolean".asJson, "question" -> a.question.asJson, "answer" -> a.answer.asJson)
  val booleanAnswerDecoder: Decoder[BooleanAnswer] = (c:HCursor) => for {
    question <- c.downField("question").as[QuestionId]
    answer <- c.downField("answer").as[Option[Boolean]]
  } yield BooleanAnswer(question=question, answer=answer)
  val videoAnswerEncoder: Encoder[VideoAnswer] = (a:VideoAnswer) => Json.obj("kind" -> "video".asJson, "question" -> a.question.asJson, "answer" -> a.answer.asJson)
  val videoAnswerDecoder: Decoder[VideoAnswer] = (c:HCursor) => for {
    question <- c.downField("question").as[QuestionId]
    answer <- c.downField("answer").as[Option[VideoResource]]
  } yield VideoAnswer(question=question, answer=answer)

  implicit val answerEncoder: Encoder[Answer] = {
    case a:ShortTextAnswer => shortTextAnswerEncoder(a)
    case a:BooleanAnswer => booleanAnswerEncoder(a)
    case a:VideoAnswer => videoAnswerEncoder(a)
  }
  implicit val answerDecoder: Decoder[Answer] = (c:HCursor) => c.downField("kind").as[String] flatMap {
    case "shortText" => shortTextAnswerDecoder(c)
    case "boolean" => booleanAnswerDecoder(c)
    case "video" => videoAnswerDecoder(c)
  }

  implicit val questionnaireTaskOutputEncoder: Encoder[QuestionnaireTaskOutput] =(q:QuestionnaireTaskOutput) => Json.obj(
    "kind" -> "questionnaire".asJson, "answers" -> q.answers.asJson
  )
  implicit val questionnaireTaskOutputDecoder: Decoder[QuestionnaireTaskOutput] = (c:HCursor) => c.downField("answers").as[Seq[Answer]].map(QuestionnaireTaskOutput.apply)


  implicit val taskOutputBodyEncoder: Encoder[TaskOutputBody] = {
    case qt:QuestionnaireTaskOutput => questionnaireTaskOutputEncoder(qt)
    case ct:Critique => critiqueEncoder(ct)
    case EmptyTaskOutputBody => Json.obj("kind" -> Json.fromString("empty"))
  }
  implicit val taskOutputBodyDecoder: Decoder[TaskOutputBody] = (c: HCursor) => c.downField("kind").as[String].flatMap {
    case "questionnaire" => questionnaireTaskOutputDecoder(c)
    case "critique" => critiqueDecoder(c)
    case "empty" => Right(EmptyTaskOutputBody)
  }

  implicit val taskOutputEncoder: Encoder[TaskOutput] = (t:TaskOutput) => Json.obj(
    "id" -> t.id.asJson, "attn" -> t.attn.asJson, "by" -> t.by.asJson, "task" -> t.task.asJson,
    "created" -> t.created.asJson, "updated" -> t.updated.asJson, "finalised" -> t.finalised.asJson,
    "body" -> t.body.asJson
  )
  implicit val taskOutputDecoder: Decoder[TaskOutput] = (c:HCursor) => for {
    id <- c.downField("id").as[TaskOutputId]
    attn <- c.downField("attn").as[Seq[Target]]
    by <- c.downField("by").as[Target]
    task <- c.downField("task").as[TaskId]
    created <- c.downField("created").as[Long]
    updated <- c.downField("updated").as[Long]
    finalised <- c.downField("finalised").as[Option[Long]]
    body <- c.downField("body").as[TaskOutputBody]
  } yield TaskOutput(id=id, attn=attn, by=by, task=task, created=created, updated=updated, finalised=finalised, body=body)

  implicit val emailAndPasswordEncoder: Encoder[EmailAndPassword] = (e:EmailAndPassword) => Json.obj(
    "email" -> e.email.asJson, "password" -> e.password.asJson
  )
  implicit val emailAndPasswordDecoder: Decoder[EmailAndPassword] = (c:HCursor) => for {
    email <- c.downField("email").as[String]
    password <- c.downField("password").as[String]
  } yield EmailAndPassword(email=email, password=password)

  implicit val passwordLoginEncoder: Encoder[PasswordLogin] = (p:PasswordLogin) => Json.obj(
    "pwhash" -> p.pwhash.asJson, "email" -> p.email.asJson, "username" -> p.username.asJson
  )
  implicit val PasswordLoginDecoder: Decoder[PasswordLogin] = (c:HCursor) => for {
    email <- c.downField("email").as[Option[String]]
    username <- c.downField("username").as[Option[String]]
    pwhash <- c.downField("pwhash").as[Option[String]]
  } yield PasswordLogin(pwhash=pwhash, email=email, username=username)

  implicit val activeSessionEncoder: Encoder[ActiveSession] = (a:ActiveSession) => Json.obj(
    "ip" -> a.ip.asJson, "key" -> a.key.asJson, "since" -> a.since.asJson
  )
  implicit val activeSessionDecoder: Decoder[ActiveSession] = (c:HCursor) => for {
    ip <- c.downField("ip").as[String]
    key <- c.downField("key").as[String]
    since <-c.downField("since").as[Long]
  } yield ActiveSession(ip=ip, key=key, since=since)

  implicit val userEncoder: Encoder[User] = (u:User) => Json.obj(
    "id" -> u.id.asJson, "activeSessions" -> u.activeSessions.asJson, "avatar" -> u.avatar.asJson,
    "identities" -> u.identities.asJson, "name" -> u.name.asJson, "nickname" -> u.nickname.asJson,
    "pwlogin" -> u.pwlogin.asJson, "secret" -> u.secret.asJson
  )
  implicit val userDecoder: Decoder[User] = (c:HCursor) => for {
    id <- c.downField("id").as[UserId]
    activeSessions <- c.downField("activeSessions").as[Seq[ActiveSession]]
    avatar <- c.downField("avatar").as[Option[String]]
    identities <- c.downField("identities").as[Seq[Identity]]
    name <- c.downField("name").as[Option[String]]
    nickname <- c.downField("nickname").as[Option[String]]
    pwlogin <- c.downField("pwlogin").as[PasswordLogin]
    secret <- c.downField("secret").as[String]
  } yield User(id=id, activeSessions=activeSessions, avatar=avatar, identities=identities, name=name, nickname=nickname, pwlogin=pwlogin, secret=secret)

  implicit def withPermsEncoder[T](implicit encoder: Encoder[T]): Encoder[WithPerms[T]] = (w:WithPerms[T]) => Json.obj(
    "item" -> w.item.asJson, "perms" -> w.perms.asJson
  )
  implicit def withPermsDecoder[T](implicit decoder: Decoder[T]): Decoder[WithPerms[T]] = (c:HCursor) => for {
    item <- c.downField("item").as[T]
    perms <- c.downField("perms").as[Map[String, Boolean]]
  } yield WithPerms(item=item, perms=perms)

  implicit val smallFileDetailsEncoder: Encoder[SmallFileDetails] = (f:SmallFileDetails) => Json.obj(
    "id" -> f.id.asJson, "course" -> f.courseId.asJson, "owner" -> f.ownerId.asJson, "mime" -> f.mime.asJson,
    "name" -> f.name.asJson, "size" -> f.size.asJson, "created" -> f.created.asJson, "updated" -> f.updated.asJson
  )
  implicit val smallFileDetailsDecoder: Decoder[SmallFileDetails] = (c:HCursor) => for {
    id <- c.downField("id").as[SmallFileId]
    course <- c.downField("course").as[CourseId]
    owner <- c.downField("owner").as[RegistrationId[Course, CourseRole, HasKind]]
    name <- c.downField("name").as[String]
    size <- c.downField("size").as[Option[Long]]
    mime <- c.downField("mime").as[Option[String]]
    created <- c.downField("created").as[Long]
    updated <- c.downField("updated").as[Long]
  } yield SmallFileDetails(id=id, courseId=course, ownerId=owner, name=name, size=size, mime=mime, created=created, updated=updated)


  implicit val allocatedCritEncoder: Encoder[AllocatedCrit] = (a:AllocatedCrit) => Json.obj(
    "target" -> a.target.asJson, "critique" -> a.critique.asJson
  )
  implicit val allocatedCritDecoder: Decoder[AllocatedCrit] = (c:HCursor) => for {
    target <- c.downField("target").as[Target]
    critique <- c.downField("critique").as[Option[TaskOutputId]]
  } yield AllocatedCrit(target=target, critique=critique)

  implicit val critAllocationEncoder: Encoder[CritAllocation] = (c:CritAllocation) => Json.obj(
    "id" -> c.id.asJson, "task" -> c.task.asJson, "allocation" -> c.allocation.asJson, "completedBy" -> c.completeBy.asJson
  )
  implicit val critAllocationDecoder: Decoder[CritAllocation] = (c:HCursor) => for {
    id <- c.downField("id").as[CritAllocationId]
    task <- c.downField("task").as[TaskId]
    completedBy <- c.downField("completedBy").as[Target]
    allocation <- c.downField("allocation").as[Seq[AllocatedCrit]]
  } yield CritAllocation(id=id, task=task, completeBy = completedBy, allocation=allocation)

  def write[T](thing: T)(implicit encoder: Encoder[T]): String = thing.asJson.toString

  def read[T](text: String)(implicit decoder: Decoder[T]): Try[T] = decode(text)(decoder).toTry

  def readF[T](text:String)(implicit decoder: Decoder[T]): Future[T] = Future.fromTry(read(text)(decoder))

  def readR[T](text:String)(implicit decoder: Decoder[T]): Ref[T] = Ref(read(text)(decoder))

  case class ParsingException(msg:String) extends RuntimeException

}

