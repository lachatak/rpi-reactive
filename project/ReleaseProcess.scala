import com.typesafe.sbt.SbtGit.git
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease._

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

  def defineVersionsByCommits(baseDir: File, baseVersion: String): ReleaseStep = { st: State =>

    def currentVersion(): Option[Version] = Some((Process("git describe --abbrev=0 --tags", baseDir) !!)).filterNot(_ == "fatal").flatMap(tag => sbtrelease.Version(tag.replaceAll("\n", "").substring(1)))

    def currentVersionSha1(currentVersion: Version): Option[String] = Some((Process(s"git rev-list -n 1 v${currentVersion.string}", baseDir) !!)).map(_.replaceAll("\n", ""))

    def releaseVersion(currentVersionSha1: String, currentVersion: Version): Option[Version] = {
      val comments = (Process(s"git log --pretty=%s $currentVersionSha1..HEAD", baseDir) !!).linesIterator.filter(it => it == '!' || it == '=' || it == '+')

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
  }

  lazy val settings =
    Seq(
      releaseProcess := Seq(
        checkSnapshotDependencies,
        defineVersionsByCommits(baseDirectory.value, git.baseVersion.value),
        setReleaseVersion,
        runClean,
        runTest,
        tagRelease,
        publishArtifacts
      )
    )
}
