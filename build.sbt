
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "assessory"
scalaVersion := "3.3.1"
organization := "org.assessory"
version := "0.4.0-SNAPSHOT"

def useScala3 = (scalaVersion := "3.3.3")
def useScala2 = (scalaVersion := "2.13.7")

lazy val commonSettings = Seq(
  organization := "org.assessory",
  version := "0.4-SNAPSHOT",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  resolvers ++= Seq(
    "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
    "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "jitpack" at "https://jitpack.io",
    DefaultMavenRepository
  ),
  libraryDependencies ++= Seq(
    //Handy
   "org.scalactic" %% "scalactic" % "3.2.9",
    "org.scalatest" %% "scalatest" % "3.2.9" % "test",
//  ("org.specs2" %% "specs2-core" % "4.8.3" % "test").cross(CrossVersion.for3Use2_13)

//    what it downloads, it'll be something like org.scalameta.munit_3.0.0-M3.jar
    "org.scalameta" %% "munit" % "0.7.29" % Test

  )

)

lazy val api = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("modules/api"))
  .settings(commonSettings:_*)
  .settings(
    useScala3,
    libraryDependencies ++= Seq(
      "com.github.wbillingsley.handy" %%% "handy" % "v0.11-SNAPSHOT",
    )
  )

lazy val apiJS = api.js
lazy val apiJVM = api.jvm

// Mongo contains the database serialisation and deserialisation
lazy val mongo = (project in file("modules/asyncmongo"))
  .dependsOn(apiJVM)
  .settings(commonSettings:_*)
  .settings(
    useScala3,
    libraryDependencies ++= Seq(
      ("org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3").cross(CrossVersion.for3Use2_13),
      "org.mindrot" % "jbcrypt" % "0.3m"
    )
  )

// Model relies on both the API and the database layer, just not the web layer
lazy val model = (project in file("modules/model"))
  .dependsOn(apiJVM, mongo)
  .settings(commonSettings:_*)
  .settings(
    useScala3,
    libraryDependencies ++= Seq(
      "net.sf.opencsv" % "opencsv" % "2.0"
    )
  )

val circeVersion = "0.14.1"
lazy val clientPickle = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("modules/clientPickle"))
  .settings(commonSettings:_*)
  .settings(
    useScala3,
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-parser"
    ).map(_ % circeVersion)
  )
  .dependsOn(api)

lazy val clientPickleJS = clientPickle.js
lazy val clientPickleJVM = clientPickle.jvm

lazy val vclient = project.in(file("modules/vclient"))
  .settings(commonSettings:_*)
  .settings(
    useScala3,
    scalaJSUseMainModuleInitializer := true,
    Test / scalaJSUseMainModuleInitializer := false,
    libraryDependencies ++= Seq(
//      ("org.scala-js" %%% "scalajs-dom" % "1.0.0",
      "com.wbillingsley" %%% "doctacular" % "0.3.0",
    )
  )
  .dependsOn(apiJS, clientPickleJS)
  .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin, ScalaJSWeb)

lazy val sjsProjects = Seq(vclient)



// The web layer
val PekkoVersion = "1.0.2"
val PekkoHttpVersion = "1.0.1"

val prod = true

lazy val pekkohttp = (project in file("modules/pekkoHttp"))
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(commonSettings:_*)
  .aggregate(sjsProjects.map(sbt.Project.projectToRef):_*)
  .settings(
    useScala3,

    libraryDependencies ++= Seq(
      // JavaScript
      "org.webjars" % "bootstrap" % "4.4.1-1",
      "org.webjars" % "font-awesome" % "4.5.0",
      "org.webjars" % "marked" % "0.3.2-1"
    ),

    scalaJSProjects := sjsProjects,
    Assets / pipelineStages := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSPipeline),
    // triggers scalaJSPipeline when using compile or continuous compilation
    Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      ("org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion),
      ("org.apache.pekko" %% "pekko-stream" % PekkoVersion),
      ("org.apache.pekko" %% "pekko-http" % PekkoHttpVersion),

      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.1"
    ),
    Assets / WebKeys.packagePrefix := "public/",
    Runtime / managedClasspath += (Assets / packageBin).value,

    if (prod) {
      (Compile / resources) += (vclient / Compile / fullOptJS).value.data
    } else {
      (Compile / resources) += (vclient / Compile / fastOptJS).value.data
    }

  ).enablePlugins(SbtWeb, JavaAppPackaging)


// A sneaky backdoor way of getting stuff into the database
lazy val cheatScript = project.in(file("modules/cheatScript"))
  .settings(commonSettings:_*)
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(
    useScala3,

    libraryDependencies ++= Seq(
//      ("org.specs2" %% "specs2-core" % "4.3.4" % "test").cross(CrossVersion.for3Use2_13),
      ("org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion),
      ("org.apache.pekko" %% "pekko-stream" % PekkoVersion),
      ("org.apache.pekko" %% "pekko-http" % PekkoHttpVersion),
    )
  )



// We also need to register munit as a test framework in sbt so that "sbt test" will work and the IDE will recognise
// tests
testFrameworks += new TestFramework("munit.Framework")

/*
lazy val play = (project in file("modules/play"))
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(commonSettings:_*)
  .aggregate(sjsProjects.map(sbt.Project.projectToRef):_*)
  .settings(
    useScala2,
    scalacOptions += "-Ytasty-reader", // Compatibility between Scala 2 and 3


    // RPM settings
    Linux / maintainer := "William Billingsley <wbillingsley@cantab.net>",

    Linux / packageSummary  := "Assessory",

    packageDescription := "Social assessment",

    rpmRelease := "1",

    rpmVendor := "assessory.org",

    rpmUrl := Some("https://github.com/assessory/atemporary.git"),

    rpmLicense := Some("MIT Licence"),

    libraryDependencies ++= Seq(
      // JavaScript
      ws,
      "org.webjars" %% "webjars-play" % "2.8.0",
      "org.webjars" % "bootstrap" % "4.4.1-1",
      "org.webjars" % "font-awesome" % "4.5.0",
      "org.webjars" % "marked" % "0.3.2-1"
    ),

    scalaJSProjects := sjsProjects,
    Assets / pipelineStages := Seq(scalaJSPipeline),
    pipelineStages := Seq(scalaJSProd, gzip),
    // triggers scalaJSPipeline when using compile or continuous compilation
    Compile / compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.4",
      guice
    )
  )
  .enablePlugins(PlayScala)






*/
