lazy val commonSettings = Seq(
  organization := "edu.berkeley.cs",
  version := "0.4.0",
  scalaVersion := "2.12.12",
  scalacOptions += "-Xsource:2.11",
  libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.4.1",
  libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3.1",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.3",
  scalaSource in Compile := baseDirectory.value / "src",
  scalaSource in Test := baseDirectory.value / "test",
)

lazy val main = (project in file(".")).
  settings(name := "kiwi-formal").
  settings(commonSettings: _*)

