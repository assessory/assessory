// The Typesafe repository
resolvers += "Typesafe repository" at "https://dl.bintray.com/typesafe/maven-releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.0.0")
addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"  % "1.0.0")
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.0")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.11")

addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                  % "1.0.2")

addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                % "1.1.4")

