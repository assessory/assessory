package com.assessory.sjsreact

import com.assessory.api.client.invalidId
import com.wbillingsley.handy.appbase.Course
import com.assessory.clientpickle.Pickles._
import com.wbillingsley.handy.Latch
import japgolly.scalajs.react.extra.router.{BaseUrl, Router}
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._


object WebApp extends JSApp {

  val mountNode = dom.document.getElementById("assessory-render")
  val root = ReactDOM.render(MainRouter.router(), mountNode)

  def rerender():Unit = root.forceUpdate()

  // Whenever a Latch in the UI completes, trigger a rerender (something has changed)
  Latch.addGlobalListener { _ => rerender() }

  @JSExport
  override def main(): Unit = {

  }


}
