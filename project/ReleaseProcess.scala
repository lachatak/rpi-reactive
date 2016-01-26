import com.typesafe.sbt.SbtGit.git
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._
import sbtrelease._

import scala.util
import scala.util.Success

object ReleaseProcess {

  def setVersionOnly(selectVersion: Versions => String): ReleaseStep = { st: State =>
    val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))

    val selected = selectVersion(vs)
    st.log.info("Setting version to '%s'." format selected)
    val useGlobal = Project.extract(st).get(releaseUseGlobalVersion)
    val versionStr = (if (useGlobal) globalVersionString else versionString) format selected

    reapply(Seq(
      if (useGlobal) version in ThisBuild := selected
      else version := selected
    ), st)
  }

  lazy val setReleaseVersion: ReleaseStep = setVersionOnly(_._1)

  def defineVersionsByCommits(baseDir: File, baseVersion: String): ReleaseStep = ReleaseStep(action = { st: State =>

    def currentVersion(): Option[Version] = {
      util.Try(Process("git describe --abbrev=0 --tags", baseDir) !!) match {
        case Success(result) => Some(result).filterNot(_ == "fatal").map(_.replaceAll("\n", "")).flatMap(tag => sbtrelease.Version(tag.substring(1)))
        case _ => None
      }
    }

    def currentVersionSha1(currentVersion: Version): Option[String] = Some((Process(s"git rev-list -n 1 v${currentVersion.string}", baseDir) !!)).map(_.replaceAll("\n", ""))

    def releaseVersion(currentVersionSha1: String, currentVersion: Version): Option[Version] = {
      val defaultChoice = extractDefault(st, "o")

      val comments = (Process(s"git log --pretty=%s $currentVersionSha1..HEAD", baseDir) !!).linesIterator

      if (comments.isEmpty) {
        defaultChoice orElse SimpleReader.readLine(s"v${currentVersion.string} is the latest tag version! No new commit to release! Overwrite or keep tag (o/k)? [o] ") match {
          case Some("" | "o" | "O") => st.log.warn("Overwriting a tag can cause problems if others have already seen the tag!")
          case Some("k" | "K") => sys.error(s"v${currentVersion.string} already exists. Aborting release!")
          case Some(x) => sys.error(s"Invalid option $x!")
        }
      }

      println(s"extracted v${currentVersion.string} with $currentVersionSha1")
      println(s"extracted command symbols since the tag: $comments")

      val nextVersion = if (comments.contains('!')) {
        currentVersion.bumpMajor.copy(minor = Some(0), bugfix = Some(0))
      } else if (comments.contains('+')) {
        currentVersion.bumpMinor.copy(bugfix = Some(0))
      } else if (comments.contains('=')) {
        currentVersion.bumpBugfix
      } else {
        currentVersion.bumpMinor.copy(bugfix = Some(0))
      }
      println(s"derived version number: v${nextVersion.string}")

      Some(nextVersion)
    }

    val releaseV = for {
      currentTagVersion <- currentVersion()
      sha1 <- currentVersionSha1(currentTagVersion)
      version <- releaseVersion(sha1, currentTagVersion)
    } yield version.string

    st.put(versions, (releaseV.getOrElse(baseVersion), ""))
  }, check = commitReleaseVersion.check)

  lazy val settings =
    Seq(
      releaseProcess := Seq(
        checkSnapshotDependencies,
        defineVersionsByCommits(baseDirectory.value, git.baseVersion.value),
        setReleaseVersion,
        runClean,
        runTest,
        tagRelease,
        publishArtifacts,
        pushChanges
      )
    )
}
