import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease._

// we hide the existing definition for setReleaseVersion to replace it with our own

object ReleaseProcess {

  val showNextVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")
  val showReleaseVersion = settingKey[String]("the future version once releaseNextVersion has been applied to it")


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
      }),
      showReleaseVersion <<= (version, releaseVersion) ((v, f) => f(v)),
      showNextVersion <<= (version, releaseNextVersion) ((v, f) => f(v)),
      // strip the qualifier off the input version, eg. 1.2.1-SNAPSHOT -> 1.2.1
      releaseVersion := { ver => sbtrelease.Version(ver).map(_.withoutQualifier.string).getOrElse(versionFormatError) },

      // bump the minor version and append '-SNAPSHOT', eg. 1.2.1 -> 1.3.0-SNAPSHOT
      releaseNextVersion := { ver => sbtrelease.Version(ver).map(_.bumpMinor.asSnapshot.string).getOrElse(versionFormatError) }
    )
}
