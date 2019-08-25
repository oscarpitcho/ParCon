name := course.value + "-" + assignment.value

scalaVersion := "0.13.0-RC1"

scalacOptions ++= Seq("-language:implicitConversions", "-deprecation")

fork := true
connectInput in run := true
outputStrategy := Some(StdoutOutput)

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % Test,
  ("com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.16" % Test).withDottyCompat(scalaVersion.value),
  ("com.typesafe.akka" %% "akka-actor-typed" % "2.5.16").withDottyCompat(scalaVersion.value)
)

assemblyMergeStrategy in assembly := {
  // This line is needed to prevent sbt-assembly from discarding Akka's
  // reference.conf when merging jars.
  case PathList("reference.conf") => MergeStrategy.concat
  // These two lines change the default behavior in case of class file
  // conflict from "complaining with an error" to "silently pick one option
  // and hope for the best". The conflict in question should go away once
  // https://github.com/lampepfl/dotty/pull/5912 is published.
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v")

// In case we want to use a Dotty version before it's fully released
resolvers += Resolver.sonatypeRepo("staging")
