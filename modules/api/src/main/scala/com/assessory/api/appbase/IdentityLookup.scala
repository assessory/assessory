package com.assessory.api.appbase

case class IdentityLookup(service:String,
                          value:Option[String],
                          username:Option[String])
