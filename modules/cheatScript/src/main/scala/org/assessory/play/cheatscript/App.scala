package org.assessory.play.cheatscript

import com.assessory.api.call.*
import com.assessory.clientpickle.CallClient

import scala.concurrent.Future

object App {

  def main(args:Array[String]): Unit = {

    println("Starting...")
    val networkService = NetworkService("http://localhost:8080/api/call")
    import networkService.given
    val client = CallClient()
    println("Created client...")

    (for 
      ReturnSession(u) <- client.register()
    yield 
      println(s)
    ) recoverWith { case ex =>
      ex.printStackTrace
      Future.failed(ex)
    }

  }

}
