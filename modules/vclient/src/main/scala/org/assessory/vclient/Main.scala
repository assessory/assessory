package org.assessory.vclient

import com.wbillingsley.veautiful.html.Attacher
import com.wbillingsley.veautiful.logging.Logger
import org.scalajs.dom

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
