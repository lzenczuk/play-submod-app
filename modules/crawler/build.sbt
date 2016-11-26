import sbt.Keys._

name := "clawler"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.12",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.12",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",

  "com.google.inject" % "guice" % "4.0",
  "net.codingwell" %% "scala-guice" % "4.0.1",

  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.mockito" % "mockito-core" % "2.2.17" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.12" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.12" % "test",
  "org.mock-server" % "mockserver-netty" % "3.10.4" % "test"
)
