package org.assessory.vclient

import com.wbillingsley.veautiful.html.*
import com.wbillingsley.veautiful.logging.Logger
import org.scalajs.dom
import scalajs.js

given marked:Markup = new Markup({ (s:String) => js.Dynamic.global.marked.parse(s).asInstanceOf[String] })

object Main {
  val logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]):Unit = {
    logger.info("Attaching router to document")
    val n = dom.document.getElementById("assessory-render")

    // Clear the loading message
    n.innerHTML = ""

    // Attach the router
    val root = Attacher.newRoot(n)
    root.render(Routing.Router)
  }

}
