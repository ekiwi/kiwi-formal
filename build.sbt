lazy val commonSettings = Seq(
  organization := "com.dkasza",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.12",
  scalacOptions += "-Xsource:2.11",
  libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.4.1",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8",
)

lazy val main = (project in file(".")).
  settings(name := "dank-formal").
  settings(commonSettings: _*)

