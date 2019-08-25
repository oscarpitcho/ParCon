name := course.value + "-" + assignment.value

scalaVersion := "0.13.0-RC1"

scalacOptions ++= Seq("-language:implicitConversions", "-deprecation")

fork := true
connectInput in run := true
outputStrategy := Some(StdoutOutput)

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % Test,
  ("org.apache.spark" %% "spark-core" % "2.4.0").withDottyCompat(scalaVersion.value),
  ("org.apache.spark" %% "spark-sql" % "2.4.0").withDottyCompat(scalaVersion.value)
)

assemblyMergeStrategy in assembly := {
  // These two lines change the default behavior in case of class file
  // conflict from "complaining with an error" to "silently pick one option
  // and hope for the best".
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")

// In case we want to use a Dotty version before it's fully released
resolvers += Resolver.sonatypeRepo("staging")
