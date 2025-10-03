//addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.3.4")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.4.1")

val SbtDevOopsVersion = "3.2.1"
addSbtPlugin("io.kevinlee" % "sbt-devoops-scala"     % SbtDevOopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-sbt-extra" % SbtDevOopsVersion)
addSbtPlugin("io.kevinlee" % "sbt-devoops-starter"   % SbtDevOopsVersion)
