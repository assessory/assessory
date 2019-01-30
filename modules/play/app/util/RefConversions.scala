package util

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.wbillingsley.handy.{Ref, RefMany}
import com.wbillingsley.handyplay.{RefEnumIter, RefEnumerator}
import play.api.libs.iteratee.{Enumeratee, Enumerator, Input, Iteratee}

import scala.concurrent.{ExecutionContext, Future}

object RefConversions {

  /**
    * Turns a RefMany into an Enumerator, which can in turn be transformed into a Publisher to connect to
    * Reactive Streams
    */
  implicit class EnumerateRefMany[T](val rm: RefMany[T]) extends AnyVal {

    def enumerate(implicit executionContext:ExecutionContext):Enumerator[T] = rm match {
      case re:RefEnumerator[T] => re.enumerator
      case rei:RefEnumIter[T] => rei.enumerator.flatMap(trav => Enumerator.enumerate(trav))

      case _ => new Enumerator[T] {
        def apply[A](it: Iteratee[T, A]) = {
          val res = rm.foldLeft(it) { (it, el) => Iteratee.flatten(it.feed(Input.El(el))) }
          res.toRefOpt.toFutureOpt.map(_.getOrElse(it))(executionContext)
        }
      }
    }

    def enumerateR(implicit executionContext:ExecutionContext):Ref[Enumerator[T]] = rm whenReady { _.enumerate }

    /**
      * Turns the RefMany into an Enumerator, and pushes it through an Enumeratee.
      * This allows for things like <code>take(n)</code>, by applying an appropriate Enumeratee.
      */
    def through[B](e:Enumeratee[T, B])(implicit executionContext:ExecutionContext):RefMany[B] = {
      new RefEnumerator[B](enumerate through e)
    }
  }


  implicit class RefManyToSource[T](val rm:RefMany[T]) extends AnyVal {

    import play.api.libs.concurrent.Execution.Implicits._

    def toFutureSource:Future[Source[T, NotUsed]] = {
      //for {
      //  enum <- rm.enumerateR.toFuture
      //} yield Source.fromPublisher(Streams.enumeratorToPublisher(enum))

      // FIXME: This isn't great, but it solves a transient bug where the generated Source was completing before the
      // last element was sent
      for {
        iter <- rm.collect.toFuture
      } yield Source.apply[T](scala.collection.immutable.Iterable(iter:_*))
    }

  }

  implicit class StringifyJson(val rm: RefMany[String]) extends AnyVal {

    import play.api.libs.concurrent.Execution.Implicits._

    def jsSource:Future[Source[String, NotUsed]] = {
      for {
        jsSource <- rm.toFutureSource
      } yield {
        Source.single("[").concat({
          var sep = ""
          for {
            j <- jsSource
          } yield {
            val s = sep + j
            sep = ","
            s
          }
        }).concat(
          Source.single("]")
        )
      }
    }
  }

}