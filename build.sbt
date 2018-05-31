val dottyVersion = "0.8.0-RC1"
val ScalaVersion = "2.11.12"
//val theScalaVersion = dottyVersion
val theScalaVersion = ScalaVersion

lazy val root = (project in file(".")).
  settings(
    organization := "io.kevinlee",
    name := "pdf2excel",
    version := "0.0.1",

    scalaVersion := theScalaVersion,

    crossScalaVersions := Seq("2.11.12", "2.12.4"),

    wartremoverErrors ++= Warts.all,

    scalacOptions ++= Seq(
      "-deprecation",             // Emit warning and location for usages of deprecated APIs.
      "-feature",                 // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked",               // Enable additional warnings where generated code depends on assumptions.
      "-Xfatal-warnings",         // Fail the compilation if there are any warnings.
      "-Xlint",                 // Enable recommended additional warnings.
      "-Ywarn-adapted-args",      // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code",         // Warn when dead code is identified.
      "-Ywarn-inaccessible",      // Warn about inaccessible types in method signatures.
      "-Ywarn-nullary-override",  // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      "-Ywarn-numeric-widen"      // Warn when numerics are widened.
    ),

    resolvers += "3rd Party Repo" at "http://dl.bintray.com/kevinlee/maven",

    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,

      "org.apache.pdfbox" % "pdfbox" % "2.0.8",

//      ("org.scalaz" %% "scalaz-core" % "7.2.23").withDottyCompat(scalaVersion.value),
//      ("info.folone" %% "poi-scala" % "0.18").withDottyCompat(scalaVersion.value),
//      ("com.lihaoyi" %% "fastparse"  % "1.0.0").withDottyCompat(scalaVersion.value),
//      ("com.github.nscala-time" %% "nscala-time" % "2.18.0").withDottyCompat(scalaVersion.value),
//      ("com.iheart" %% "ficus" % "1.4.3").withDottyCompat(scalaVersion.value),
//      ("io.kevinlee" %% "skala" % "0.1.0").withDottyCompat(scalaVersion.value)

      "org.scalaz" %% "scalaz-core" % "7.2.20",
      "info.folone" %% "poi-scala" % "0.18",
      "com.lihaoyi" %% "fastparse" % "1.0.0",
      "com.github.nscala-time" %% "nscala-time" % "2.18.0",
      "com.iheart" %% "ficus" % "1.4.3",
      "io.kevinlee" %% "skala" % "0.1.0"
    )
  )
