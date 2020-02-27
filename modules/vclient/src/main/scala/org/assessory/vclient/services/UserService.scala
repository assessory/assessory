package org.assessory.vclient.services

import com.assessory.api.client.EmailAndPassword
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.User
import org.assessory.vclient.Routing
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object UserService {

  val cache = mutable.Map.empty[String, Latch[User]]

  val self:Latch[Option[User]] = Latch.lazily(
    Ajax.post("/api/self", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[User]).optional404
  )

  def approval:Ref[Approval[User]] = Approval(
    for {
      opt <- self.request.toRef
      u <- opt.toRef
    } yield u
  )

  def logOut():Unit = {
    Ajax.post("/api/logOut", headers=AJAX_HEADERS).andThen{
      case _ =>
        self.fill(None)
        Routing.Router.routeTo(Routing.Home)
    }
  }

  def logIn(ep:EmailAndPassword):Future[User] = {
    Ajax.post("/api/logIn", Pickles.write(ep), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[User]).andThen {
      case Success(u) =>
        self.fill(Some(u))
        Routing.Router.routeTo(Routing.Home)
    }
  }

  def signUp(ep:EmailAndPassword):Future[User] = {
    Ajax.post("/api/signUp", Pickles.write(ep), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[User]).andThen{
      case Success(u) =>
        self.fill(Some(u))
        Routing.Router.routeTo(Routing.Home)
    }
  }

  def loadId[KK <: String](id:Id[User,KK]):Latch[User] = Latch.lazily(
    Ajax.get(s"/api/user/${id.id}", headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[User])
  )

  val lu = new LookUp[User, String] {
    override def one[KK <: String](r: Id[User, KK]): Ref[User] = cache.getOrElseUpdate(r.id, loadId(r)).request.toRef

    override def many[KK <: String](r: Ids[User, KK]): RefMany[User] = ???
  }

}
