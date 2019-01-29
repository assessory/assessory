package com.assessory.sjsreact.services

import com.assessory.api.client.EmailAndPassword
import com.assessory.clientpickle.Pickles
import Pickles._
import com.assessory.sjsreact.{MainRouter, WebApp}
import com.wbillingsley.handy.Ref._
import com.wbillingsley.handy._
import com.wbillingsley.handy.appbase.User
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
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
        MainRouter.goTo(MainRouter.Home)
        WebApp.rerender()
    }
  }

  def logIn(ep:EmailAndPassword):Future[User] = {
    Ajax.post("/api/logIn", Pickles.write(ep), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[User]).andThen {
      case Success(u) =>
        self.fill(Some(u))
        MainRouter.goTo(MainRouter.Home)
        WebApp.rerender()
    }
  }

  def signUp(ep:EmailAndPassword):Future[User] = {
    Ajax.post("/api/signUp", Pickles.write(ep), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[User]).andThen{
      case Success(u) =>
        self.fill(Some(u))
        MainRouter.goTo(MainRouter.Home)
        WebApp.rerender()
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
