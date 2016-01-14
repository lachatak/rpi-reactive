import sbt.Keys._
import sbtassembly.AssemblyKeys._

object Assembly {

  lazy val webAssemblySettings =
  Seq(
    mainClass in assembly := Some("org.kaloz.rpi.reactive.RpiReactiveApp"),
    assemblyJarName in assembly := "web.jar"
  )

}
