package org.assessory.vclient.user

import com.wbillingsley.handy.Ref
import com.wbillingsley.handy.appbase.User
import com.wbillingsley.veautiful.html.<
import org.assessory.vclient.services.UserService

object UserViews {

  def name(u:User):String = {
    u.name.orElse(u.pwlogin.email.orElse(u.identities.find(_.username.nonEmpty).flatMap(_.username))).getOrElse("Can't find a name")
  }

}
