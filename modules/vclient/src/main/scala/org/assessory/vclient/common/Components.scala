package org.assessory.vclient.common

import com.wbillingsley.handy.Latch
import com.wbillingsley.veautiful.DiffNode
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode}
import org.scalajs.dom.{Element, Node}

import scala.util.{Failure, Success}

object Components {

  case class LatchRender[T](latch: Latch[T])(
                           some: T => DiffNode[Element, Node],
                           none: => DiffNode[Element, Node] = <.div(),
                           error: Throwable => DiffNode[Element, Node] = x => <.div(x.getMessage)
  ) extends VHtmlComponent {

    val listener:Latch.Listener[T] = { _ => rerender() }

    override def afterAttach(): Unit = {
      super.afterAttach()
      latch.addListener(listener)
    }

    override protected def render: DiffNode[Element, Node] = {
      latch.request.value match {
        case Some(Success(v)) => some(v)
        case Some(Failure(x)) => error(x)
        case _ => none
      }
    }

    override def beforeDetach(): Unit = {
      super.beforeDetach()
      latch.removeListener(listener)
    }

  }


}
