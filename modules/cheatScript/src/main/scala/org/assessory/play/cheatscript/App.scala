package org.assessory.play.cheatscript

import com.assessory.api.call.GetSession


object App {

  def main(args:Array[String]): Unit = {

    for {
      c <- CommandClient.open("http://localhost:9000/api/call")

    } {
      c.close()
    }
  }

}
