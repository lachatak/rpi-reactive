import sbt._
import sbt.Keys._

object Version {

  val akka          = "2.4.16"
  val jodaTime      = "2.8.2"
  val scalazCore    = "7.2.8"
  val scalaTest     = "3.0.1"
  val config        = "1.2.1"
  val mockito       = "1.10.19"
  val macwire       = "1.0.5"
  val jnativehook   = "2.0.2"
  val scalaPool     = "0.4.0"
  val scalaLogging  = "3.5.0"
  val logBack       = "1.1.3"
  val scalaAsync    = "0.9.5"
  val scalaRx       = "0.26.5"
}

object Library {
  val akkaActor       = "com.typesafe.akka"          %% "akka-actor"                    % Version.akka
  val akkaSlf4j       = "com.typesafe.akka"          %% "akka-slf4j"                    % Version.akka
  val akkaTestkit     = "com.typesafe.akka"          %% "akka-testkit"                  % Version.akka
  val akkaStreams     = "com.typesafe.akka"          %% "akka-stream"                   % Version.akka
  val jodaTime        = "joda-time"                  %  "joda-time"                     % Version.jodaTime
  val config          = "com.typesafe" 		           %  "config" 			                  % Version.config
  val scalazCore      = "org.scalaz"           	     %% "scalaz-core"                   % Version.scalazCore
  val mockito         = "org.mockito"                %  "mockito-core"                  % Version.mockito
  val scalaTest       = "org.scalatest"              %% "scalatest"                     % Version.scalaTest
  val jnativehook     = "com.1stleg"                 %  "jnativehook"                   % Version.jnativehook
  val scalaPool       = "io.github.andrebeat"        %% "scala-pool"                    % Version.scalaPool
  val scalaLogging    = "com.typesafe.scala-logging" %% "scala-logging"                 % Version.scalaLogging
  val logBack         = "ch.qos.logback"             % "logback-classic"                % Version.logBack
//  val scalaAsync      = "org.scala-lang.modules"     % "scala-async_2.11"               % Version.scalaAsync
  val scalarx         = "io.reactivex"               %% "rxscala"                       % Version.scalaRx
  val scodecBits      = "org.scodec"                 %% "scodec-bits"                   % "1.1.4"
  val scodecCore      = "org.scodec"                 %% "scodec-core"                   % "1.10.3"
  val scodeAkka       = "org.scodec"                 %% "scodec-akka"                   % "0.3.0"
  val cats            = "org.typelevel"              %% "cats"                          % "0.9.0"
  val monixEval       = "io.monix"                   %% "monix-eval"                    % "2.2.1"
  val monixCats       = "io.monix"                   %% "monix-cats"                    % "2.2.1"
}

object Dependencies {

  import Library._

  val reactive = deps(
    akkaActor,
    akkaSlf4j,
    scalaPool,
    akkaStreams,
    scodecBits,
    scodecCore,
    scodeAkka,
    cats,
    monixEval,
    monixCats,
//    scalaAsync,
    scalarx,
    config,
    scalaLogging,
    logBack,
    scalazCore,
    mockito       	% "test",
    akkaTestkit     % "test",
    scalaTest     	% "test"
  )

  val examples = deps(
    akkaActor,
    akkaSlf4j,
    jnativehook,
    config,
    scalazCore,
    mockito       	% "test",
    akkaTestkit     % "test",
    scalaTest     	% "test"
  )

  private def deps(modules: ModuleID*): Seq[Setting[_]] = Seq(libraryDependencies ++= modules)
}
