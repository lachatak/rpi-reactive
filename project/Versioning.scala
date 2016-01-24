import com.typesafe.sbt.SbtGit.git

object Versioning {

  val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r    


  lazy val settings = Seq(
    git.baseVersion := "1.0.0",
    git.useGitDescribe := true
    //    git.gitTagToVersionNumber := {
    //      case VersionRegex(v, "SNAPSHOT") => Some(s"$v-SNAPSHOT")
    //      case VersionRegex(v, "") => Some(v)
    //      case VersionRegex(v, s) => Some(s"$v-$s-SNAPSHOT")
    //      case _ => None
    //    }
  )
}
