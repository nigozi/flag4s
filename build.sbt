import sbt.url
import ReleaseTransformations._

name := "flag4s"

ThisBuild / scalaVersion := "2.13.11"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation"
)

lazy val publishSettings = Seq(
  organization := "io.nigo",
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(ScmInfo(url("https://github.com/nigozi/flag4s"), "git@github.com:nigozi/flag4s.git")),
  licenses := List("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/nigozi/flag4s")),
  developers := List(Developer("nigozi", "Nima Goodarzi", "nima@nigo.io", url("https://github.com/nigozi"))),
  Test / publishArtifact := false,
  sonatypeProfileName := "io.nigo",
  publishMavenStyle := true
)

lazy val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    setReleaseVersion,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeReleaseAll"),
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion
  )
)

val akkaVersion = "2.6.20"
val akkaHttpVersion = "10.2.7"
val akkaHttpCirceVersion = "1.40.0-RC3"
val catsVersion = "2.8.0"
val catsEffectVersion = "3.4.5"
val circeVersion = "0.14.5"
val http4sVersion = "0.23.18"
val http4sBlazeVersion = "0.23.14"
val pureConfigVersion = "0.17.2"
val redisClientVersion = "3.42"
val scalaTestVersion = "3.2.16"
val scalaMockVersion = "5.2.0"

val commonDependencies = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalamock" %% "scalamock" % scalaMockVersion % Test
)

lazy val root = project
  .in(file("."))
  .settings(publishSettings ++ releaseSettings)
  .aggregate(
    core,
    http4s,
    akka
  )

lazy val core = project
  .in(file("core"))
  .settings(
    publishSettings ++
      releaseSettings ++
      List(
        description := "flag4s core",
        name := "flag4s-core",
        libraryDependencies ++=
          commonDependencies ++
            testDependencies ++
            List(
              "org.http4s" %% "http4s-core" % http4sVersion,
              "org.http4s" %% "http4s-blaze-client" % http4sBlazeVersion,
              "org.http4s" %% "http4s-dsl" % http4sVersion,
              "org.http4s" %% "http4s-circe" % http4sVersion,
              "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
              "net.debasishg" %% "redisclient" % redisClientVersion
            )
      )
  )

lazy val http4s = project
  .in(file("http4s-api"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    publishSettings ++
      releaseSettings ++
      List(
        description := "flag4s api for http4s",
        name := "flag4s-api-http4s",
        libraryDependencies ++=
          commonDependencies ++
            testDependencies ++
            Seq(
              "org.http4s" %% "http4s-core" % http4sVersion,
              "org.http4s" %% "http4s-blaze-server" % http4sBlazeVersion,
              "org.http4s" %% "http4s-dsl" % http4sVersion,
              "org.http4s" %% "http4s-circe" % http4sVersion
            )
      )
  )

lazy val akka = project
  .in(file("akka-http-api"))
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    publishSettings ++
      releaseSettings ++
      List(
        description := "akka http api for http4s",
        name := "flag4s-api-akka-http",
        libraryDependencies ++=
          commonDependencies ++
            testDependencies ++
            Seq(
              "com.typesafe.akka" %% "akka-actor" % akkaVersion,
              "com.typesafe.akka" %% "akka-stream" % akkaVersion,
              "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion,
              "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
              "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion,
              "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
              "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
            )
      )
  )
