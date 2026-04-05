ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "phishing-filter",

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",

      "org.http4s" %% "http4s-ember-server" % "0.23.27",
      "org.http4s" %% "http4s-ember-client" % "0.23.27",
      "org.http4s" %% "http4s-dsl" % "0.23.27",
      "org.http4s" %% "http4s-circe" % "0.23.27",

      "io.circe" %% "circe-core" % "0.14.9",
      "io.circe" %% "circe-generic" % "0.14.9",
      "io.circe" %% "circe-parser" % "0.14.9",

      "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-h2" % "1.0.0-RC4",
      "com.h2database" % "h2" % "2.2.224",

      "com.typesafe" % "config" % "1.4.3",

      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    ),

    assembly / assemblyJarName := "phishing-filter.jar",

    assembly / mainClass := Some("pl.play.phishingfilter.Main"),

    assembly / test := {}
  )