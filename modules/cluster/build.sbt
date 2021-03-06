import sbt.Keys._

name := "cluster"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.12",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.12",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.12",
  "com.google.inject" % "guice" % "4.0",
  "net.codingwell" %% "scala-guice" % "4.0.1",

  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.mockito" % "mockito-core" % "2.2.17" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % "test"
)
