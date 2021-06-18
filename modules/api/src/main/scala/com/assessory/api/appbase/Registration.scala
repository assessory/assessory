package com.assessory.api.appbase

import com.wbillingsley.handy._

case class Registration[T, R, P <: HasKind](

  id: RegistrationId[T, R, P],

  user: UserId,

  target: Id[T, String],

  roles: Set[R] = Set.empty,

  provenance: P,

  updated:Long = System.currentTimeMillis,

  created:Long = System.currentTimeMillis

) extends HasId[RegistrationId[T, R, P]]

case class RegistrationId[T, R, P <: HasKind](id:String) extends Id[Registration[T, R, P], String]