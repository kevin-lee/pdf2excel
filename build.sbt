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

    crossScalaVersions := Seq("2.11.12", "2.12.4"),

    resolvers += "3rd Party Repo" at "http://dl.bintray.com/kevinlee/maven",

    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test,

      "org.apache.pdfbox" % "pdfbox" % "2.0.8",

//      ("org.scalaz" %% "scalaz-core" % "7.2.20").withDottyCompat(),
//      ("info.folone" %% "poi-scala" % "0.18").withDottyCompat(),
//      ("com.lihaoyi" %% "fastparse"  % "1.0.0").withDottyCompat(),
//      ("com.github.nscala-time" %% "nscala-time" % "2.18.0").withDottyCompat(),
//      ("com.iheart" %% "ficus" % "1.4.3").withDottyCompat(),
//      ("io.kevinlee" %% "skala" % "0.0.9").withDottyCompat()

      "org.scalaz" %% "scalaz-core" % "7.2.20",
      "info.folone" %% "poi-scala" % "0.18",
      "com.lihaoyi" %% "fastparse" % "1.0.0",
      "com.github.nscala-time" %% "nscala-time" % "2.18.0",
      "com.iheart" %% "ficus" % "1.4.3",
      "io.kevinlee" %% "skala" % "0.0.9"
    )
  )
