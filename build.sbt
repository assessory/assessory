
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val commonSettings = Seq(
  // Gets snapshots from first resolver. TODO: Remove
  updateOptions := updateOptions.value.withLatestSnapshots(false),

  scalaVersion := "2.12.8",
  organization := "com.impressory",
  version := "0.3-SNAPSHOT",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    DefaultMavenRepository
  ),
  libraryDependencies ++= Seq(
    //Handy
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )

)


name := "atemporary"
  
organization := "com.impressory"
  
scalaVersion := "2.12.8"

version := "1.0.0-SNAPSHOT"

lazy val api = (crossProject(JSPlatform, JVMPlatform) in file("modules/api"))
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "com.wbillingsley" %%% "handy" % "0.9.0-SNAPSHOT",
      "com.wbillingsley" %%% "handy-appbase" % "0.9.0-SNAPSHOT"
    )
  )

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Mongo contains the database serialisation and deserialisation
lazy val mongo = (project in file("modules/asyncmongo"))
  .dependsOn(apiJVM)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "1.2.1",
      "com.wbillingsley" %% "handy-user" % "0.9.0-SNAPSHOT"
    )
  )

// Model relies on both the API and the database layer, just not the web layer
lazy val model = (project in file("modules/model"))
  .dependsOn(apiJVM, mongo)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.sf.opencsv" % "opencsv" % "2.0",
      "org.specs2" %% "specs2-core" % "4.3.4" % "test"
    )
  )

lazy val clientPickle = (crossProject(JSPlatform, JVMPlatform) in file("modules/clientPickle"))
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      // Pickling
      "com.lihaoyi" %%% "upickle" % "0.7.1",
      "com.github.benhutchison" %%% "prickle" % "1.1.13"
    )
  )
  .dependsOn(api)

lazy val clientPickleJS = clientPickle.js
lazy val clientPickleJVM = clientPickle.jvm

lazy val reactjs = project.in(file("modules/reactjs"))
  .settings(commonSettings:_*)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false
  )
  .dependsOn(apiJS, clientPickleJS)

lazy val sjsProjects = Seq(reactjs)

// The web layer
lazy val play = (project in file("modules/play"))
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(commonSettings:_*)
  .aggregate(sjsProjects.map(sbt.Project.projectToRef):_*)
  .settings(
    // RPM settings
    maintainer in Linux := "William Billingsley <wbillingsley@cantab.net>",

    packageSummary in Linux := "Assessory",

    packageDescription := "Social assessment",

    rpmRelease := "1",

    rpmVendor := "assessory.org",

    rpmUrl := Some("https://github.com/assessory/atemporary.git"),

    rpmLicense := Some("MIT Licence"),

    libraryDependencies ++= Seq(
      // JavaScript
      ws,
      "org.webjars" %% "webjars-play" % "2.6.3",
      "org.webjars" % "bootstrap" % "3.1.1-2",
      "org.webjars" % "font-awesome" % "4.5.0",
      "org.webjars" % "marked" % "0.3.2-1"
    ),

    scalaJSProjects := sjsProjects,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSProd, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.2"
    )
  )
  .enablePlugins(PlayScala)

// A sneaky backdoor way of getting stuff into the database
lazy val cheatScript = project.in(file("modules/cheatScript"))
  .settings(commonSettings:_*)
  .dependsOn(apiJVM, mongo, model)
  .settings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.3.4" % "test"
    )
  )
