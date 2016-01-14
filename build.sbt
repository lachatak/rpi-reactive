lazy val root = Project("root", file("."))
  .aggregate(reactive, examples)
  .settings(BaseSettings.settings: _*)


lazy val reactive = Project("reactive", file("reactive"))
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.reactive: _*)
  .settings(Testing.settings: _*)

lazy val examples = Project("examples", file("examples"))
  .dependsOn(reactive)
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.examples: _*)
  .settings(Testing.settings: _*)