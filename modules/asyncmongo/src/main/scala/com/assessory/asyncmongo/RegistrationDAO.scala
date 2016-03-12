package com.assessory.asyncmongo

import com.assessory.api.wiring.RegistrationProvider
import com.assessory.asyncmongo.converters.BsonHelpers._
import com.assessory.asyncmongo.converters.RegistrationB
import com.wbillingsley.handy.Id._
import com.wbillingsley.handy._
import Ref._
import com.wbillingsley.handy.appbase._
import org.mongodb.scala.bson._

class RegistrationDAO[T, R, P <: HasKind](collName:String, r:RegistrationB[T, R, P])
  extends DAO(clazz=classOf[Registration[T,R,P]], collName=collName, converter=r.read)
  with RegistrationProvider[T, R, P] {

  override def byUserAndTarget(user: Id[User, String], target: Id[T, String]): Ref[Registration[T, R, P]] = {
    findOne(("user" $eq user) and ("target" $eq target))
  }

  def byTarget(target:Id[T, String]) = findMany("target" $eq target)

  def byTargets(targets:Ids[T, String]) = findMany(Document("target" -> $in(targets)))

  def byTargets(targets:Seq[Id[T, String]]) = findMany(Document("target" -> $in(targets)))

  def byUser(user:Id[User, String]) = findMany("user" $eq user)

  def saveSafe(c:Registration[T,R,P]) = {
    findAndReplace("_id" $eq c.id, r.write(c), upsert=true).toRef
  }

  def register(user:Id[User, String], target:Id[T, String], roles:Set[R], provenance:P) = {
    updateAndFetch(
      query = ("user" $eq user) and ("target" $eq target),
      update = $addToSet("roles" -> toBsonSeq(roles.toSeq)(r.rToFromBson))
    ) orIfNone saveSafe(
      Registration[T,R,P](
        id = allocateId.asId,
        user = user,
        target = target,
        roles = roles,
        provenance = provenance
      )
    )
  }

}

object RegistrationDAO {

  val course = new RegistrationDAO[Course, CourseRole, HasKind]("courseRegistration", RegistrationB.courseRegB)

  val group = new RegistrationDAO[Group, GroupRole, HasKind]("groupRegistration", RegistrationB.groupRegB)

}
