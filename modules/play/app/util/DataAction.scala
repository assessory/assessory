package util

import java.util.NoSuchElementException

import akka.NotUsed
import akka.stream.scaladsl.{Concat, Flow, Source}
import com.assessory.asyncmongo.UserDAO
import com.wbillingsley.handy.appbase.UserError
import com.wbillingsley.handy.{RefMany, Ref, Approval}
import Ref._
import com.wbillingsley.handyplay.{RefEnumIter, RefEnumerator, UserFromRequest}
import org.reactivestreams.Publisher
import play.api.http.HttpEntity.Streamed
import play.api.libs.iteratee.{Enumeratee, Input, Iteratee, Enumerator}
import play.api.libs.json.Json
import play.api.libs.streams.Streams
import play.api.mvc._
import java.util.NoSuchElementException

import scala.concurrent.{ExecutionContext, Future}


class AppbaseRequest[A](request:Request[A]) extends WrappedRequest(request) {

  lazy val user = UserDAO.user(request)

  lazy val approval = new Approval(user)

  val sessionKey = request.session.get("sessionKey").getOrElse(AppbaseRequest.newSessionKey)
}

object AppbaseRequest {
  def newSessionKey = java.util.UUID.randomUUID.toString
}


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
          val res = rm.fold(it) { (it, el) => Iteratee.flatten(it.feed(Input.El(el))) }
          res.toFutOpt.map(_.getOrElse(it))(executionContext)
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
      for {
        enum <- rm.enumerateR.toFuture
      } yield Source.fromPublisher(Streams.enumeratorToPublisher(enum))
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


object UserAction extends ActionBuilder[AppbaseRequest] with ActionTransformer[Request, AppbaseRequest] {

  import play.api.libs.concurrent.Execution.Implicits._

  def transform[A](request: Request[A]) = Future.successful {
    new AppbaseRequest(request)
  }

  def invokeBlock[A](request: Request[A])(block:AppbaseRequest[A] => Future[Result]) = {
    (for {
      r <- transform(request)
      res <- block(r)
    } yield res.addingToSession("sessionKey" -> r.sessionKey)(r)).recover({
      case e:NoSuchElementException => Results.NotFound(Json.obj("error" -> e.getMessage))
      case UserError(e) => Results.BadRequest(Json.obj("error" -> e))
    })

  }

  implicit def rtf(rr:Ref[Result]):Future[Result] = {
    rr
      .orIfNone(Results.NotFound(Json.obj("error" -> "Not found")).itself)
      .recoverWith({
        case UserError(e) => Results.BadRequest(Json.obj("error" -> e)).itself
      })
      .toFuture
  }



}

