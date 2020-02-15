package util

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.wbillingsley.handy.{Ref, RefMany}

import scala.concurrent.{ExecutionContext, Future}

object RefConversions {


  implicit class RefManyToSource[T](val rm:RefMany[T]) extends AnyVal {

    def toFutureSource(implicit ec:ExecutionContext):Future[Source[T, NotUsed]] = {
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

    def jsSource(implicit ec:ExecutionContext):Future[Source[String, NotUsed]] = {
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