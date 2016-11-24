name := "play-submod-app"

version := "1.0"

scalaVersion := "2.11.8"

lazy val cluster = Project(id="cluster", base = file("modules/cluster"))

lazy val crawler = Project(id="crawler", base = file("modules/crawler"))

lazy val admin = Project(id="admin", base = file("modules/admin"))
  .enablePlugins(PlayScala)
  .dependsOn(cluster)
  .dependsOn(cluster)

