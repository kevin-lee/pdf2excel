ThisBuild / organization       := props.Org
ThisBuild / version            := props.ProjectVersion
ThisBuild / scalaVersion       := props.ProjectScalaVersion
ThisBuild / crossScalaVersions := Set(props.ProjectScalaVersion, "2.13.15").toList

lazy val root = (project in file("."))
  .settings(
    name                 := "pdf2excel",
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("3."))
        List.empty[String]
      else
        List("-Xsource:3")
    },
    Compile / run / fork := true,
    wartremoverErrors ++= Warts.allBut(Wart.ListUnapply, Wart.Any, Wart.Nothing),
    libraryDependencies ++= List(
      libs.pdfbox,
      libs.poiScala,
      libs.catsParse,
      libs.nscalaTime,
      libs.scalaCollectionCompat,
    ) ++
      List(libs.cats, libs.catsEffect) ++
      libs.effectieAll ++
      libs.loggerFAll ++
      libs.extrasAll ++
      List(
        libs.pureconfig(scalaVersion.value),
        libs.logback,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.20.0"
      ),
    testFrameworks ~= (testFws => TestFramework("hedgehog.sbt.Framework") +: testFws),
  )

lazy val props = new {
  //  final val ScalaVersion    = "3.0.0"
  final val ScalaVersion        = "3.3.4"
  final val ProjectScalaVersion = ScalaVersion

  final val Org            = "io.kevinlee"
  final val ProjectVersion = "0.1.0"

  final val CatsVersion       = "2.10.0"
  final val CatsEffectVersion = "3.5.4"
  final val PureConfigVersion = "0.17.7"
  final val LogbackVersion    = "1.4.8"
  final val NewtypeVersion    = "0.4.4"

  final val HedgehogVersion = "0.10.1"

  final val EffectieVersion = "2.0.0-beta14"
  final val LoggerFVersion  = "2.0.0-beta24"

  val ExtrasVersion = "0.44.0"

  final val PdfboxVersion     = "2.0.28"
  final val PoiScalaVersion   = "0.23"
  final val CatsParseVersion  = "0.3.9"
  final val NscalaTimeVersion = "2.32.0"

  final val ScalaCollectionCompatVersion = "2.12.0"

  val DeclineVersion = "2.4.1"
}

lazy val libs = new {

  lazy val cats       = "org.typelevel" %% "cats-core"   % props.CatsVersion
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % props.CatsEffectVersion

  lazy val effectieAll = List(
    "io.kevinlee" %% "effectie-core"         % props.EffectieVersion,
    "io.kevinlee" %% "effectie-syntax"       % props.EffectieVersion,
    "io.kevinlee" %% "effectie-cats-effect3" % props.EffectieVersion
  )
  lazy val loggerFAll  = List(
    "io.kevinlee" %% "logger-f-cats"  % props.LoggerFVersion,
    "io.kevinlee" %% "logger-f-slf4j" % props.LoggerFVersion
  )

  lazy val extrasAll = List(
    "io.kevinlee" %% "extras-cats"   % props.ExtrasVersion,
    "io.kevinlee" %% "extras-render" % props.ExtrasVersion,
  )

  lazy val pdfbox     = "org.apache.pdfbox"       % "pdfbox"      % props.PdfboxVersion
  lazy val poiScala   = "info.folone"            %% "poi-scala"   % props.PoiScalaVersion
  lazy val catsParse  = "org.typelevel"          %% "cats-parse"  % props.CatsParseVersion
  lazy val nscalaTime = "com.github.nscala-time" %% "nscala-time" % props.NscalaTimeVersion

  lazy val scalaCollectionCompat =
    "org.scala-lang.modules" %% "scala-collection-compat" % props.ScalaCollectionCompatVersion

  def pureconfig(scalaVersion: String): ModuleID =
    if (scalaVersion.startsWith("3."))
      "com.github.pureconfig" %% "pureconfig-core" % props.PureConfigVersion
    else
      "com.github.pureconfig" %% "pureconfig"      % props.PureConfigVersion

  lazy val logback = "ch.qos.logback" % "logback-classic" % props.LogbackVersion

}
