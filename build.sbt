
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "assessory"
scalaVersion := "3.0.0"
organization := "org.assessory"
version := "0.4.0-SNAPSHOT"

def useScala3 = (scalaVersion := "3.0.0")

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
    ("org.specs2" %% "specs2-core" % "4.8.3" % "test").cross(CrossVersion.for3Use2_13)
  )

)



lazy val api = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("modules/api"))
  .settings(commonSettings:_*)
  .settings(
    useScala3,
    libraryDependencies ++= Seq(
      "com.github.wbillingsley.handy" %%% "handy" % "v0.11-SNAPSHOT",
//      "com.github.wbillingsley.handy" %%% "handy-appbase" % "v0.10-SNAPSHOT"
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
      "com.github.wbillingsley.veautiful" %%% "veautiful" % "master-SNAPSHOT",
      "com.github.wbillingsley.veautiful" %%% "veautiful-templates" % "master-SNAPSHOT",
    )
  )
  .dependsOn(apiJS, clientPickleJS)
  .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)

lazy val sjsProjects = Seq(vclient)




/*



// The web layer
lazy val play = (project in file("modules/play"))
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(commonSettings:_*)
  .aggregate(sjsProjects.map(sbt.Project.projectToRef):_*)
  .settings(
    useScala2,

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

// A sneaky backdoor way of getting stuff into the database
lazy val cheatScript = project.in(file("modules/cheatScript"))
  .settings(commonSettings:_*)
  .dependsOn(apiJVM, mongo, model, clientPickleJVM)
  .settings(
    useScala2,
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.3.4" % "test",
      "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.1.0-M4"
    )
  )

*/
