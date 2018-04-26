organization := "nigo.io"

name := "flag4s-api-http4s"

version := "0.1"

scalaVersion := "2.12.5"

scalacOptions += "-Ypartial-unification"

val circeVersion = "0.9.3"
val http4sVersion = "0.18.9"

  libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.1.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.http4s" %% "http4s-core" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "com.github.sebruck" %% "scalatest-embedded-redis" % "0.3.0" % Test
)
