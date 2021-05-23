val ScalaVersion = "2.13.6"
val theScalaVersion = ScalaVersion

val CatsVersion = "2.2.0"
val CatsEffectVersion = "2.2.0"
val PureConfigVersion = "0.13.0"
val LogbackVersion = "1.2.3"
val newtypeVersion = "0.4.4"

val hedgehogVersion = "0.7.0"

val EffectieVersion = "1.11.0"
val LoggerFVersion = "1.11.0"

val cats = "org.typelevel" %% "cats-core" % CatsVersion
val catsEffect = "org.typelevel" %% "cats-effect" % CatsEffectVersion
val newtype = "io.estatico" %% "newtype" % newtypeVersion
lazy val effectieCatsEffect = "io.kevinlee" %% "effectie-cats-effect" % EffectieVersion
lazy val loggerFCatsEffectSlf4j = Seq(
  "io.kevinlee" %% "logger-f-cats-effect" % LoggerFVersion,
  "io.kevinlee" %% "logger-f-log4s" % LoggerFVersion
)

ThisBuild / organization := "io.kevinlee"
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := theScalaVersion
ThisBuild / crossScalaVersions := Set(theScalaVersion, "2.12.13").toList

lazy val root = (project in file("."))
  .settings(
    name := "pdf2excel",
    wartremoverErrors ++= Warts.allBut(Wart.ListUnapply),
    libraryDependencies ++= Seq(
        "org.apache.pdfbox" % "pdfbox" % "2.0.18",

        "info.folone" %% "poi-scala" % "0.20",
        "org.typelevel" %% "cats-parse" % "0.3.3",
        "com.github.nscala-time" %% "nscala-time" % "2.28.0",
        "com.iheart" %% "ficus" % "1.5.0",
        "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4",
      ) ++
      Seq(cats, catsEffect, effectieCatsEffect, newtype) ++
      loggerFCatsEffectSlf4j ++
      Seq(
        "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion
      ),
    testFrameworks += TestFramework("hedgehog.sbt.Framework"),
  )
