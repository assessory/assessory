package org.assessory.play.cheatscript

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.assessory.api.call.{Call, GetSession, Return, ReturnSession}
import com.wbillingsley.handy.Ref
import play.api.libs.ws.ahc._
import Ref._
import com.wbillingsley.handy.appbase.ActiveSession

import com.assessory.clientpickle.CallPickles._
import play.api.libs.ws.DefaultBodyReadables._
import play.api.libs.ws.DefaultBodyWritables._
import scala.concurrent.ExecutionContext.Implicits._

object CommandClient {

  implicit val system = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  implicit val materializer = ActorMaterializer()

  val wsClient = StandaloneAhcWSClient()

  def close() = {
    wsClient.close()
    system.terminate()
  }

  def open(url:String):Ref[CommandClient] = {

    (for {
      response <- wsClient.url(url).post(write(GetSession)).toRef
      body = response.body
      ReturnSession(s) <- readReturn(body).toRef
    } yield {
      println("Created new session")
      println(s)
      new CommandClient(url, s)
    }).require

  }

}

class CommandClient(url:String, s:ActiveSession) {

  def call(c:Call):Ref[Return] = {
    for {
      response <- CommandClient.wsClient.url(url).post(write(GetSession)).toRef
      body = response.body
      r <- readReturn(body).toRef
    } yield r
  }

  def close() = {
    println("Terminating connection")
    CommandClient.close()
  }

}
