package com.assessory.sjsreact.user

import com.assessory.api.client.WithPerms
import com.assessory.sjsreact.CommonComponent
import com.assessory.sjsreact.services.UserService
import com.wbillingsley.handy.{Ref, Id}
import com.wbillingsley.handy.appbase.{Group, User}
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object UserViews {

  def userNameRef(r:Ref[User]) = CommonComponent.refNode({
    for { u <- r } yield <.span(name(u))
  })

  def name(u:User) = {
    u.name.orElse(u.pwlogin.email.orElse(u.identities.find(_.username.nonEmpty).flatMap(_.username))).getOrElse("Can't find a name")
  }

  val nameById = ReactComponentB[Id[User,String]]("User name by ID").render_P(id => <.span(userNameRef(UserService.lu.one(id)))).build

}
