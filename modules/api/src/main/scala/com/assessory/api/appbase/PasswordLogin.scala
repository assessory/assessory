package com.assessory.api.appbase

trait PasswordLoginT {


  val pwhash: Option[String]

  val username: Option[String]

  val email: Option[String]

}

case class PasswordLogin (

 pwhash: Option[String] = None,

 username: Option[String] = None,

 email:Option[String] = None

) extends PasswordLoginT
