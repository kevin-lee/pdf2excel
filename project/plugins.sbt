//addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.3.4")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.1.3")

val SbtDevOopsVersion = "2.24.0"
addSbtPlugin("io.kevinlee" % "sbt-devoops-scala"     % SbtDevOopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-sbt-extra" % SbtDevOopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-starter"   % SbtDevOopsVersion)
