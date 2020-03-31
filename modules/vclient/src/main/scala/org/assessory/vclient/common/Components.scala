package org.assessory.vclient.common

import com.wbillingsley.handy.Latch
import com.wbillingsley.veautiful.{DiffNode, Update}
import com.wbillingsley.veautiful.html.{<, VHtmlComponent, VHtmlNode, ^}
import org.scalajs.dom.{Element, Event, Node, html}

import scala.util.{Failure, Success}

object Components {

  case class LatchRender[T](latch: Latch[T], _key: String = "")(
                           some: T => DiffNode[Element, Node],
                           none: => DiffNode[Element, Node] = <.div(),
                           error: Throwable => DiffNode[Element, Node] = x => <.div(x.getMessage)
  ) extends VHtmlComponent with Update {

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

  implicit class EventMethods(val e:Event) extends AnyVal {

    def inputValue:Option[String] = e.target match {
      case h:html.Input => Some(h.value)
      case t:html.TextArea => Some(t.value)
      case _ => None
    }

    def checked:Option[Boolean] = e.target match {
      case h:html.Input => Some(h.checked)
      case _ => None
    }

  }

  def latchErrorRender[T](latch:Latch[T]):LatchRender[T] = {
    LatchRender(latch)(
      some = { _ => <.span() },
      none = { <.span() },
      error = { x => <("label")(^.cls := "text-danger", "Error: " + x.getMessage)}
    )
  }


}
