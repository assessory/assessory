package org.assessory.play.controllers

import javax.inject.{Inject, Singleton}

import com.assessory.asyncmongo._
import com.assessory.model.DoWiring
import com.wbillingsley.handy.RefFuture
import play.api._

@Singleton
class StartupSettings @Inject() (environment:Environment) {

  println("Applied settings")

  DB.dbName = "assessory_2017_1"

  // Set the execution context (ie the thread pool) that RefFuture work should happen on
  RefFuture.executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  // Wire up the lookups
  DoWiring.doWiring

}

