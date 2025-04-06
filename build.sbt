ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "untitled"
  )

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.2",
  "com.h2database" % "h2" % "2.3.232",
  "org.scalafx" %% "scalafx" % "21.0.0-R32",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)