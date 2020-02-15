libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % "1.6.0",
  "com.github.japgolly.scalajs-react" %%% "extra" % "1.6.0"
)

jsDependencies ++= Seq(

  "org.webjars.bower" % "react" % "0.14.3"
    /        "react-with-addons.js"
    minified "react-with-addons.min.js"
    commonJSName "React",

  "org.webjars.bower" % "react" % "0.14.3"
    /         "react-dom.js"
    minified  "react-dom.min.js"
    dependsOn "react-with-addons.js"
    commonJSName "ReactDOM")

