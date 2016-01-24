import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease._

// we hide the existing definition for setReleaseVersion to replace it with our own

object ReleaseProcess {

  def setVersionOnly(selectVersion: Versions => String): ReleaseStep = { st: State =>
    val vs = st.get(ReleaseKeys.versions).getOrElse(sys.error("No versions are set! Was this release part executed before inquireVersions?"))

    st.log.info(s"version $vs")

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

  lazy val settings =
    Seq(
      releaseProcess := Seq(
        checkSnapshotDependencies,
        inquireVersions,
        setReleaseVersion,
        runTest,
        tagRelease
//        pushChanges
      ),
      releaseVersion <<= (releaseVersionBump) (bumper => {
        ver => sbtrelease.Version(ver)
          .map(_.withoutQualifier)
          .map(_.bump(bumper).string).getOrElse(versionFormatError)
      })
    )
}
