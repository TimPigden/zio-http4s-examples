

name := "zio-http4s-examples"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers += Resolver.sonatypeRepo("releases")

inThisBuild(Seq(
  version := "0.7",
  isSnapshot := true,
  scalaVersion := "2.12.11"
))

lazy val myScalacOptions = Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-language:postfixOps",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ywarn-value-discard",
  "-Ypartial-unification")

lazy val scalaTestVersion = "3.0.8"

lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
lazy val json4s =  "org.json4s" %% "json4s-jackson" % "3.6.6"

lazy val cats = "org.typelevel" %% "cats-core" % "2.1.1"
lazy val catsEffect =  "org.typelevel" %% "cats-effect" % "2.1.1"
val commonsIo = "commons-io" % "commons-io" % "2.4"
val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

val magnolia = "com.propensive" %% "magnolia" % "0.12.0"

lazy val avro = "org.apache.avro" % "avro" % "1.9.1"
lazy val snappy = "org.xerial.snappy" % "snappy-java" % "1.1.7.3"

lazy val Http4sVersion = "0.21.3"
lazy val http4sBlazeServer = "org.http4s"  %% "http4s-blaze-server" % Http4sVersion
lazy val http4sBlazeClient = "org.http4s"  %% "http4s-blaze-client" % Http4sVersion
lazy val http4sDsl = "org.http4s"      %% "http4s-dsl"          % Http4sVersion


// lazy val `zio-kafka-version` = "0.4.1+34-69f19fa2+20191205-1812"
lazy val zioVersion = "1.0.0-RC18-2"
lazy val `zio-interop` = "2.0.0.0-RC12"

lazy val zio = "dev.zio" %% "zio" %  zioVersion
lazy val `zio-streams` = "dev.zio" %% "zio-streams" % zioVersion
// lazy val `zio-kafka` = "dev.zio" %% "zio-kafka"   % `zio-kafka-version`
lazy val `zio-test` = "dev.zio" %% "zio-test" % zioVersion % "test"
lazy val `zio-test-sbt` = "dev.zio" %% "zio-test-sbt" % zioVersion % "test"
lazy val `zio-interop-shared` = "dev.zio" %% "zio-interop-shared" % zioVersion
lazy val `zio-interop-cats` = "dev.zio" %% "zio-interop-cats" % `zio-interop`


lazy val commonSettings = Seq(
  parallelExecution in Test := false,
  scalacOptions ++= myScalacOptions,
  organization := "com.optrak",
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
  exportJars := true,
  updateOptions := updateOptions.value.withCachedResolution(true),
  libraryDependencies ++= Seq(
    `zio-interop-cats`,
    zio,
    `zio-test`,
    `zio-test-sbt`
  )
)

lazy val `http4s1` = (project in file ("http4s1"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    http4sBlazeServer,
    http4sBlazeClient,
    http4sDsl,
    catsEffect,
    scalaXml,
    `zio-test`
  ))

  /*
lazy val `avro-magnolia` = (project in file ("avro-magnolia"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    commonsIo,
    logback,
    scalaTest,
    json4s,
    avro,
    snappy,
    magnolia,
  )
)


lazy val `streams` = (project in file ("streams"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    http4sBlazeServer,
    http4sBlazeClient,
    http4sDsl,
    catsEffect,
    scalaXml,
    `zio-kafka`,
    `zio-streams`,
    `zio-test`
  ))

*/

