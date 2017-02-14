
lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  organization := "com.impressory",
  version := "0.2-SNAPSHOT",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    DefaultMavenRepository
  ),
  libraryDependencies ++= Seq(
    //Handy
    "org.scalactic" %% "scalactic" % "2.2.6",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

)


name := "atemporary"
  
organization := "com.impressory"
  
scalaVersion := "2.11.7"

version := "1.0.0-SNAPSHOT"

lazy val api = (crossProject.crossType(CrossType.Pure) in file("modules/api"))
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "com.wbillingsley" %%% "handy" % "0.8.0-SNAPSHOT",
      "com.wbillingsley" %%% "handy-appbase" % "0.8.0-SNAPSHOT"
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Mongo contains the database serialisation and deserialisation
lazy val mongo = (project in file("modules/asyncmongo"))
  .dependsOn(apiJVM)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "1.2.1",
      "com.wbillingsley" %% "handy-user" % "0.8.0-SNAPSHOT",
      "com.wbillingsley" %% "handy-play" % "0.8.0-SNAPSHOT"
    )
  )

// Model relies on both the API and the database layer, just not the web layer
lazy val model = (project in file("modules/model"))
  .dependsOn(apiJVM, mongo)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.sf.opencsv" % "opencsv" % "2.0",
      "org.specs2" %% "specs2" % "2.3.12" % "test"
    )
  )

lazy val clientPickle = (crossProject.crossType(CrossType.Pure) in file("modules/clientPickle"))
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      // Pickling
      "com.lihaoyi" %%% "upickle" % "0.3.9",
      "com.github.benhutchison" %%% "prickle" % "1.1.10"
    )
  )
  .jsConfigure(_ enablePlugins ScalaJSPlay)
  .dependsOn(api)

lazy val clientPickleJS = clientPickle.js
lazy val clientPickleJVM = clientPickle.jvm

lazy val reactjs = project.in(file("modules/reactjs"))
  .settings(commonSettings:_*)
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .settings(
    persistLauncher := true,
    persistLauncher in Test := false
  )
  .dependsOn(apiJS, clientPickleJS)

lazy val sjsProjects = Seq(reactjs)

// The web layer
lazy val play = (project in file("modules/play"))
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(commonSettings:_*)
  .aggregate(sjsProjects.map(sbt.Project.projectToRef):_*)
  .settings(
    libraryDependencies ++= Seq(
      // JavaScript
      ws,
      "org.webjars" %% "webjars-play" % "2.4.0-2",
      "org.webjars" % "bootstrap" % "3.1.1-2",
      "org.webjars" % "font-awesome" % "4.5.0",
      "org.webjars" % "marked" % "0.3.2-1"
    ),

    scalaJSProjects := sjsProjects,
    pipelineStages := Seq(scalaJSProd, gzip),
    libraryDependencies ++= Seq(
      "com.vmunier" %% "play-scalajs-scripts" % "0.4.0"
    )
  )
  .enablePlugins(PlayScala)

// A sneaky backdoor way of getting stuff into the database
lazy val cheatScript = project.in(file("modules/cheatScript"))
  .settings(commonSettings:_*)
  .dependsOn(apiJVM, mongo, model)
  .settings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2" % "2.3.12" % "test"
    )
  )
