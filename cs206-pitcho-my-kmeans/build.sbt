scalaVersion := "0.13.0-RC1"

scalacOptions ++= Seq("-language:implicitConversions", "-deprecation")

fork := true
connectInput in run := true
outputStrategy := Some(StdoutOutput)

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")

// In case we want to use a Dotty version before it's fully released
resolvers += Resolver.sonatypeRepo("staging")

libraryDependencies += ("com.storm-enroute" %% "scalameter-core" % "0.10.1").withDottyCompat(scalaVersion.value)
