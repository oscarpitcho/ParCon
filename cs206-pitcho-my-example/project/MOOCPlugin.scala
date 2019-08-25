package ch.epfl.lamp

import sbt._
import sbt.Keys._

/**
  * Settings shared by the student build and the teacher build
  */
object MOOCPlugin extends AutoPlugin {

  object autoImport {
    val course = SettingKey[String]("course")

    val assignment = SettingKey[String]("assignment")

    val commonSourcePackages = SettingKey[Seq[String]]("commonSourcePackages")
  }

  override def trigger = allRequirements

  override val globalSettings: Seq[Def.Setting[_]] = Seq(
    // In case we want to use an sbt-dotty version before it's fully released
    resolvers += Resolver.sonatypeRepo("staging")
  )

  override val projectSettings: Seq[Def.Setting[_]] = Seq(
    parallelExecution in Test := false
  )
}
