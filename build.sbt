organization := "nigo.io"

name := "flag4s"

version := "0.1"

val circeVersion = "0.9.3"
val http4sVersion = "0.18.9"

scalacOptions in ThisBuild += "-Ypartial-unification"

lazy val root = project
  .in(file("."))
  .aggregate(
    core,
    http4s
  )

lazy val core = project
  .in(file("core"))
  .settings(
    description := "Core flag4s library",
    name := "flag4s-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.1.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.9.1",
      "net.debasishg" %% "redisclient" % "3.5",
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.scalamock" %% "scalamock" % "4.1.0" % Test,
      "com.github.sebruck" %% "scalatest-embedded-redis" % "0.3.0" % Test
    )
  )

lazy val http4s = project
  .in(file("http4s-api"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(
  description := "Core flag4s library",
  name := "flag4s-api-http4s",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "1.1.0",
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.http4s" %% "http4s-core" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test,
    "com.github.sebruck" %% "scalatest-embedded-redis" % "0.3.0" % Test
  )
)
