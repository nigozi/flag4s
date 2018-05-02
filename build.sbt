import sbt.url
import ReleaseTransformations._

scalaVersion := "2.12.6"

name := "flag4s"

scalacOptions in ThisBuild += "-Ypartial-unification"

lazy val publishSettings = Seq(
  organization := "io.nigo",
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
    else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(ScmInfo(url("https://github.com/nigozi/flag4s"), "git@github.com:nigozi/flag4s.git")),
  licenses := List("MIT" -> url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/nigozi/flag4s")),
  developers := List(Developer("nigozi", "Nima Goodarzi", "nima@nigo.io", url("https://github.com/nigozi"))),
  publishArtifact in Test := false,
  sonatypeProfileName := "io.nigo",
  publishMavenStyle := true
)

lazy val releaseSettings = Seq (
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
            "org.http4s" %% "http4s-blaze-client" % http4sVersion,
            "org.http4s" %% "http4s-dsl" % http4sVersion,
            "org.http4s" %% "http4s-circe" % http4sVersion,
            "com.github.pureconfig" %% "pureconfig" % "0.9.1",
            "net.debasishg" %% "redisclient" % "3.5"
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
                "org.http4s" %% "http4s-blaze-server" % http4sVersion,
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
            "com.typesafe.akka" %% "akka-http" % "10.1.1",
            "de.heikoseeberger" %% "akka-http-circe" % "1.20.1",
            "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test,
          )
    )
  )
