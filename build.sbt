
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
    "com.wbillingsley" %% "handy" % "0.8.0-SNAPSHOT",
    "com.wbillingsley" %% "handy-appbase" % "0.8.0-SNAPSHOT",
    "org.scalactic" %% "scalactic" % "2.2.6",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

)


name := "atemporary"
  
organization := "com.impressory"
  
scalaVersion := "2.11.7"

version := "1.0.0-SNAPSHOT"

lazy val api = (project in file("modules/api"))
  .settings(commonSettings:_*)

lazy val mongo = (project in file("modules/asyncmongo"))
  .dependsOn(api)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.0",
      "com.wbillingsley" %% "handy-user" % "0.8.0-SNAPSHOT",
      "com.wbillingsley" %% "handy-play" % "0.8.0-SNAPSHOT"
    )
  )

lazy val model = (project in file("modules/model"))
  .dependsOn(api, mongo)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.sf.opencsv" % "opencsv" % "2.0",
      "org.specs2" %% "specs2" % "2.3.12" % "test"
    )
  )

lazy val play = (project in file("modules/play"))
  .dependsOn(api, mongo)
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      // JavaScript
      "org.webjars" %% "webjars-play" % "2.4.0-2",
      "org.webjars" % "bootstrap" % "3.1.1-2",
      "org.webjars" % "font-awesome" % "4.5.0"
    )
  )
  .enablePlugins(PlayScala)


lazy val cheatScript = project.in(file("modules/cheatScript"))
  .settings(commonSettings:_*)
  .dependsOn(api, mongo, model)
  .settings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2" % "2.3.12" % "test"
    )
  )
