import com.typesafe.sbt.SbtGit.git

object Versioning {

  lazy val settings = Seq(
    git.baseVersion := "1.0.0",
    git.useGitDescribe := true
  )

}
