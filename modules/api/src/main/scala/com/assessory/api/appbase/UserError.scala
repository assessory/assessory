package com.assessory.api.appbase

/** An error by the user. */
case class UserError(msg:String) extends Exception(msg)
