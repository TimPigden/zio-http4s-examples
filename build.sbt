

name := "zio-http4s-examples"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers += Resolver.sonatypeRepo("releases")

inThisBuild(Seq(
  version := "0.5.3",
  isSnapshot := true,
  scalaVersion := "2.12.9"
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

lazy val cats = "org.typelevel" %% "cats-core" % "1.6.1"
lazy val catsEffect =  "org.typelevel" %% "cats-effect" % "1.3.1"

lazy val Http4sVersion = "0.20.10"
lazy val http4sBlazeServer = "org.http4s"  %% "http4s-blaze-server" % Http4sVersion
lazy val http4sBlazeClient = "org.http4s"  %% "http4s-blaze-client" % Http4sVersion
lazy val http4sDsl = "org.http4s"      %% "http4s-dsl"          % Http4sVersion
lazy val http4sCirce = "org.http4s"      %% "http4s-circe"          % Http4sVersion

lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test
lazy val scalaTestNotTest = "org.scalatest" %% "scalatest" % scalaTestVersion

lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
lazy val scalaCheckNotTest = "org.scalacheck" %% "scalacheck" % "1.14.0"
lazy val scalaCheckShapeless = "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.8"

lazy val `zio-version` = "1.0.0-RC11-1"
lazy val `zio-interop` = "2.0.0.0-RC2"
lazy val zio = "dev.zio" %% "zio" %  `zio-version`
lazy val `zio-test` = "dev.zio" %% "zio-test" % `zio-version` % "test"
lazy val `zio-interop-shared` = "dev.zio" %% "zio-interop-shared" % `zio-version`
lazy val `zio-interop-cats` = "dev.zio" %% "zio-interop-cats" % `zio-interop`

lazy val testDependencies = Seq(libraryDependencies ++= Seq(
  scalaTest,
  scalaCheck,
  scalaCheckShapeless
)
)

lazy val commonSettings = Seq(
  parallelExecution in Test := false,
  scalacOptions ++= myScalacOptions,
  organization := "com.optrak",
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
  exportJars := true,
  updateOptions := updateOptions.value.withCachedResolution(true),
  libraryDependencies ++= Seq(
    `zio-interop-cats`,
    zio,
    scalaTest,
  )
)

lazy val `samples` = (project in file ("samples"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    http4sBlazeServer,
    http4sBlazeClient,
    http4sDsl,
    catsEffect,
    scalaXml,
    `zio-test`
  ))


