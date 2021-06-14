ThisBuild / organization := props.Org
ThisBuild / version := props.ProjectVersion
ThisBuild / scalaVersion := props.ProjectScalaVersion
ThisBuild / crossScalaVersions := Set(props.ProjectScalaVersion, "2.13.6", "2.12.14").toList

lazy val root = (project in file("."))
  .settings(
    name := "pdf2excel",
    wartremoverErrors ++= Warts.allBut(Wart.ListUnapply),
    libraryDependencies ++= List(
      libs.pdfbox,
      libs.poiScala,
      libs.catsParse,
      libs.nscalaTime,
      libs.ficus,
      libs.scalaCollectionCompat
    ) ++
      List(libs.cats, libs.catsEffect, libs.effectieCatsEffect, libs.newtype) ++
      libs.loggerFCatsEffectSlf4j ++
      List(
        libs.pureconfig,
        libs.logback
      ),
    testFrameworks ~= (testFws => TestFramework("hedgehog.sbt.Framework") +: testFws),
  )

lazy val props = new {
  //  final val ScalaVersion    = "3.0.0"
  final val ScalaVersion        = "2.13.6"
  final val ProjectScalaVersion = ScalaVersion

  final val Org            = "io.kevinlee"
  final val ProjectVersion = "0.1.0"

  final val CatsVersion       = "2.6.1"
  final val CatsEffectVersion = "2.5.1"
  final val PureConfigVersion = "0.16.0"
  final val LogbackVersion    = "1.2.3"
  final val NewtypeVersion    = "0.4.4"

  final val HedgehogVersion = "0.7.0"

  final val EffectieVersion = "1.11.0"
  final val LoggerFVersion  = "1.11.0"

  final val PdfboxVersion     = "2.0.18"
  final val PoiScalaVersion   = "0.20"
  final val CatsParseVersion  = "0.3.3"
  final val NscalaTimeVersion = "2.28.0"
  final val FicusVersion      = "1.5.0"

  final val ScalaCollectionCompatVersion = "2.4.4"
}

lazy val libs = new {

  lazy val cats       = "org.typelevel" %% "cats-core"   % props.CatsVersion
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % props.CatsEffectVersion
  lazy val newtype    = "io.estatico"   %% "newtype"     % props.NewtypeVersion

  lazy val effectieCatsEffect     = "io.kevinlee"            %% "effectie-cats-effect" % props.EffectieVersion
  lazy val loggerFCatsEffectSlf4j = List(
    "io.kevinlee" %% "logger-f-cats-effect" % props.LoggerFVersion,
    "io.kevinlee" %% "logger-f-log4s"       % props.LoggerFVersion
  )
  lazy val pdfbox                 = "org.apache.pdfbox"       % "pdfbox"               % props.PdfboxVersion
  lazy val poiScala               = "info.folone"            %% "poi-scala"            % props.PoiScalaVersion
  lazy val catsParse              = "org.typelevel"          %% "cats-parse"           % props.CatsParseVersion
  lazy val nscalaTime             = "com.github.nscala-time" %% "nscala-time"          % props.NscalaTimeVersion
  lazy val ficus                  = "com.iheart"             %% "ficus"                % props.FicusVersion
  lazy val scalaCollectionCompat  =
    "org.scala-lang.modules" %% "scala-collection-compat" % props.ScalaCollectionCompatVersion
  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig"      % props.PureConfigVersion
  lazy val logback    = "ch.qos.logback"         % "logback-classic" % props.LogbackVersion

}
