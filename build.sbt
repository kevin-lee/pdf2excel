val dottyVersion = "0.6.0-RC1"
val ScalaVersion = "2.11.12"
//val theScalaVersion = dottyVersion
val theScalaVersion = ScalaVersion

lazy val root = (project in file(".")).
  settings(
    organization := "io.kevinlee",
    name := "pdf2excel",
    version := "0.0.1",

    scalaVersion := theScalaVersion,
    resolvers += "3rd Party Repo" at "http://dl.bintray.com/kevinlee/maven",

    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,

      "org.apache.pdfbox" % "pdfbox" % "2.0.8",
//      ("info.folone" %% "poi-scala" % "0.18").withDottyCompat(),
//      ("com.lihaoyi" %% "fastparse"  % "1.0.0").withDottyCompat(),
//
//      ("io.kevinlee" %% "skala" % "0.0.8").withDottyCompat()
      "info.folone" %% "poi-scala" % "0.18",
      "com.lihaoyi" %% "fastparse" % "1.0.0",
      "com.github.nscala-time" %% "nscala-time" % "2.18.0",
      "com.iheart" %% "ficus" % "1.4.3",
        "io.kevinlee" %% "skala" % "0.0.8"
    )
  )
