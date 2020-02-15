package org.assessory.play.controllers

import javax.inject.{Inject, Singleton}
import com.assessory.asyncmongo._
import com.assessory.model.DoWiring
import com.wbillingsley.handy.RefFuture
import play.api._

import scala.concurrent.ExecutionContext

@Singleton
class StartupSettings @Inject() (environment:Environment)(ec: ExecutionContext) {

  println("Applied settings")

  DB.dbName = "assessory_2019_1"

  // Set the execution context (ie the thread pool) that RefFuture work should happen on
  RefFuture.executionContext = ec

  // Wire up the lookups
  DoWiring.doWiring

}

