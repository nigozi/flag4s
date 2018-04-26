organization := "nigo.io"

name := "flag4s"

version := "0.1"

lazy val root = project
  .in(file("."))
  .aggregate(
    core,
    api_http4s
  )

lazy val core = project.in(file("flag4s-core"))

lazy val api_http4s = project
  .in(file("flag4s-api-http4s"))
  .dependsOn(
    core
  )
