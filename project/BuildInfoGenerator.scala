import com.typesafe.sbt.SbtGit.git
import sbt.Keys._
import sbtbuildinfo.BuildInfoKeys._
import sbtbuildinfo.BuildInfoOption
import sbtbuildinfo.BuildInfoPlugin.autoImport.BuildInfoKey

object BuildInfoGenerator {

  lazy val buildInfoGeneratorSettings =
    Seq(
      buildInfoKeys ++= Seq[BuildInfoKey](
        name,
        version,
        git.gitHeadCommit,
        git.gitCurrentBranch,
        BuildInfoKey.action("buildTime") {
          System.currentTimeMillis
        }
      ),
      buildInfoPackage := "org.kaloz.rpio.reactive",
      buildInfoOptions += BuildInfoOption.ToJson
    )
}
