import sbt._
import Keys._
import net.virtualvoid.sbt.graph.Plugin

object BaseSettings {

  lazy val settings =
    Seq(
      organization := "org.kaloz.rpi.reactive",
      description := "Raspberry Reactive Library",
      scalaVersion := "2.12.1",
      crossPaths := false,
      homepage := Some(url("http://kaloz.org")),
      scalacOptions := Seq(
        "-encoding", "utf8",
        "-feature",
        "-unchecked",
        "-deprecation",
        "-target:jvm-1.8",
        "-language:postfixOps",
        "-language:implicitConversions"
      ),
      javacOptions := Seq(
        "-Xlint:unchecked",
        "-Xlint:deprecation"
      ),
      shellPrompt := { s => "[" + scala.Console.BLUE + Project.extract(s).currentProject.id + scala.Console.RESET + "] $ " }
    ) ++
      ResolverSettings.settings ++
      Testing.settings ++
      Plugin.graphSettings

}
