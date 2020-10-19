//val dottyVersion = "0.20.0"
//val ScalaVersion = "2.13.2"
val ScalaVersion = "2.12.12"
//val theScalaVersion = dottyVersion
val theScalaVersion = ScalaVersion

val CatsVersion = "2.2.0"
val CatsEffectVersion = "2.2.0"
val PureConfigVersion = "0.13.0"
val LogbackVersion = "1.2.3"
val newtypeVersion = "0.4.4"

val hedgehogVersion = "0.5.1"

val EffectieVersion = "1.4.0"
val LoggerFVersion = "1.3.1"

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
ThisBuild / crossScalaVersions := Set(theScalaVersion, "2.11.12", "2.12.11", "2.13.2").toList

lazy val root = (project in file("."))
  .settings(
      name := "pdf2excel"
    , wartremoverErrors ++= Warts.all
    , scalacOptions ++= Seq(
        "-deprecation"             // Emit warning and location for usages of deprecated APIs.
      , "-feature"                 // Emit warning and location for usages of features that should be imported explicitly.
      , "-unchecked"               // Enable additional warnings where generated code depends on assumptions.
      , "-Xfatal-warnings"         // Fail the compilation if there are any warnings.
      , "-Xlint"                   // Enable recommended additional warnings.
      , "-Ywarn-adapted-args"      // Warn if an argument list is modified to match the receiver.
      , "-Ywarn-dead-code"         // Warn when dead code is identified.
      , "-Ywarn-inaccessible"      // Warn about inaccessible types in method signatures.
      , "-Ywarn-nullary-override"  // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      , "-Ywarn-numeric-widen"     // Warn when numerics are widened.
      ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),

    resolvers += "3rd Party Repo" at "https://dl.bintray.com/kevinlee/maven",
    libraryDependencies ++= Seq(
        "org.apache.pdfbox" % "pdfbox" % "2.0.18",

//        ("org.scalaz" %% "scalaz-core" % "7.2.23").withDottyCompat(scalaVersion.value),
//        ("info.folone" %% "poi-scala" % "0.18").withDottyCompat(scalaVersion.value),
//        ("com.lihaoyi" %% "fastparse"  % "1.0.0").withDottyCompat(scalaVersion.value),
//        ("com.github.nscala-time" %% "nscala-time" % "2.18.0").withDottyCompat(scalaVersion.value),
//        ("com.iheart" %% "ficus" % "1.4.3").withDottyCompat(scalaVersion.value),
//        ("io.kevinlee" %% "skala" % "0.1.0").withDottyCompat(scalaVersion.value),

        "org.scalaz" %% "scalaz-core" % "7.2.30",
        "info.folone" %% "poi-scala" % "0.19",
        "com.lihaoyi" %% "fastparse" % "1.0.0",
        "com.github.nscala-time" %% "nscala-time" % "2.22.0",
        "com.iheart" %% "ficus" % "1.4.7",
        "io.kevinlee" %% "skala" % "0.1.0",
      ) ++
      Seq(cats, catsEffect, effectieCatsEffect, newtype) ++
      loggerFCatsEffectSlf4j ++
      Seq(
        "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
        "ch.qos.logback" % "logback-classic" % LogbackVersion
      ),
    testFrameworks += TestFramework("hedgehog.sbt.Framework"),
  )
