lazy val commonSettings = Seq(
  organization := "edu.berkeley.cs",
  version := "0.5-SNAPSHOT",
  scalaVersion := "2.13.5",
  scalacOptions ++= Seq("-deprecation", "-feature", "-language:reflectiveCalls"),
  // Chisel Snapshot Releases
  resolvers ++= Seq(Resolver.sonatypeRepo("snapshots")),
  libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5-SNAPSHOT",
  libraryDependencies += "edu.berkeley.cs" %% "treadle" % "1.5-SNAPSHOT",
  libraryDependencies += "edu.berkeley.cs" %% "maltese-smt" % "0.5-SNAPSHOT",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.3",
  // JNA for SMT Solver bindings
  libraryDependencies += "net.java.dev.jna" % "jna" % "5.6.0",
  libraryDependencies += "net.java.dev.jna" % "jna-platform" % "5.6.0",
  scalaSource in Compile := baseDirectory.value / "src",
  scalaSource in Test := baseDirectory.value / "test",
)

lazy val main = (project in file(".")).
  settings(name := "kiwi-formal").
  settings(commonSettings: _*)

