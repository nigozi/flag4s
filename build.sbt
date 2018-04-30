organization := "nigo.io"

name := "flag4s"

version := "0.1"

scalacOptions in ThisBuild += "-Ypartial-unification"

val catsVersion = "1.1.0"
val circeVersion = "0.9.3"
val http4sVersion = "0.18.9"

val commonDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test
)

lazy val root = project
  .in(file("."))
  .aggregate(
    core,
    http4s
  )

lazy val core = project
  .in(file("core"))
  .settings(
    description := "flag4s core",
    name := "flag4s-core",
    libraryDependencies ++=
      commonDependencies ++
        testDependencies ++
        Seq(
          "org.http4s" %% "http4s-core" % http4sVersion,
          "org.http4s" %% "http4s-blaze-client" % http4sVersion,
          "org.http4s" %% "http4s-dsl" % http4sVersion,
          "org.http4s" %% "http4s-circe" % http4sVersion,
          "com.github.pureconfig" %% "pureconfig" % "0.9.1",
          "net.debasishg" %% "redisclient" % "3.5"
        )
  )

lazy val http4s = project
  .in(file("http4s-api"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    description := "flag4s api for http4s",
    name := "flag4s-api-http4s",
    libraryDependencies ++=
      commonDependencies ++
        testDependencies ++
        Seq(
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion
    )
  )
