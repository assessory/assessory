package org.assessory.vclient.services

import com.assessory.api.client.EmailAndPassword
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles.*
import com.wbillingsley.handy.{Approval, Id, Latch, Ref, RefMany, refOps}
import com.assessory.api.appbase.{UserId, User}
import com.assessory.api.call.{ReturnUser, UserCall, StandardReturn}
import com.wbillingsley.handy.LookUp$package.EagerLookUpOne
import org.assessory.vclient.Routing
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object UserService {

  val cache = mutable.Map.empty[String, Latch[User]]

  val self:Latch[Option[User]] = Latch.lazily(
    callClient.makeCall(UserCall.WhoAmI) map {
      case ReturnUser(u) => Some(u)
      case StandardReturn.ReturnNone => None
    }
  )

  def approval:Ref[Approval[User]] = Approval(
    for {
      opt <- self.request.toRef
      u <- opt.toRefOpt
    } yield u
  )

  def logOut():Unit = {
    callClient.logout().andThen{
      case _ =>
        self.fill(None)
        Routing.Router.routeTo(Routing.Home)
    }
  }

  def logIn(ep:EmailAndPassword):Future[User] = {
    callClient.login(ep.email, ep.password).andThen {
      case Success(u) =>
        self.fill(Some(u))
        Routing.Router.routeTo(Routing.Home)
    }
  }

  def signUp(ep:EmailAndPassword):Future[User] = {
    callClient.register(ep.email, ep.password).andThen{
      case Success(u) =>
        self.fill(Some(u))
        Routing.Router.routeTo(Routing.Home)
    }
  }

  def loadId[KK <: String](id:Id[User,KK]):Latch[User] = Latch.lazily(
    for ReturnUser(u) <- callClient.makeCall(UserCall.GetUser(UserId(id.id))) yield u
  )

  given EagerLookUpOne[Id[User, String], User] = (r:Id[User, String]) =>
    cache.getOrElseUpdate(r.id, loadId(r)).request.toRef


}
