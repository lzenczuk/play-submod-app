name := "play-submod-app"

version := "1.0"

scalaVersion := "2.11.8"

lazy val admin = Project(id="admin", base = file("modules/admin"))
  .enablePlugins(PlayScala)

