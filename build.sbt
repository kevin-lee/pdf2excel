ThisBuild / organization       := props.Org
ThisBuild / version            := props.ProjectVersion
ThisBuild / scalaVersion       := props.ProjectScalaVersion
ThisBuild / crossScalaVersions := Set(props.ProjectScalaVersion, "2.13.16").toList

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
      libs.kittens,
    ) ++
      libs.refined(scalaVersion.value) ++
      List(libs.cats, libs.catsEffect) ++
      libs.effectieAll ++
      libs.loggerFAll ++
      libs.extrasAll ++
      libs.declineAll ++
      List(
        libs.pureconfig(scalaVersion.value),
        libs.logback,
        libs.log4JToSlf4J
      ),
    testFrameworks ~= (testFws => TestFramework("hedgehog.sbt.Framework") +: testFws),
  )

lazy val props = new {
  final val ScalaVersion        = "3.3.5"
  final val ProjectScalaVersion = ScalaVersion

  final val Org            = "io.kevinlee"
  final val ProjectVersion = "0.1.0"

  val Refined4sVersion = "0.19.0"

  val RefinedVersion = "0.11.2"

  val CatsVersion       = "2.13.0"
  val CatsEffectVersion = "3.5.7"

  val KittensVersion = "3.4.0"

  val PureConfigVersion    = "0.17.8"
  val LogbackVersion       = "1.5.16"
  final val NewtypeVersion = "0.4.4"

  final val HedgehogVersion = "0.10.1"

  val EffectieVersion = "2.0.0"
  val LoggerFVersion  = "2.1.2"

  val Log4JToSlf4JVersion = "2.24.3"

  val ExtrasVersion = "0.44.0"

  val PdfboxVersion     = "2.0.33"
  val PoiScalaVersion   = "0.25"
  val CatsParseVersion  = "1.1.0"
  val NscalaTimeVersion = "3.0.0"

  val ScalaCollectionCompatVersion = "2.13.0"

  val DeclineVersion = "2.5.0"
}

lazy val libs = new {

  def refined(scalaVersion: String): List[ModuleID] =
    if (scalaVersion.startsWith("3."))
      refined4sAll ++ List("io.kevinlee" %% "refined4s-refined-compat-scala3" % props.Refined4sVersion)
    else
      ("io.estatico" %% "newtype" % "0.4.4") :: refinedAll ++
        List("io.kevinlee" %% "refined4s-refined-compat-scala2" % props.Refined4sVersion)

  lazy val refined4sAll =
    List(
      "io.kevinlee" %% "refined4s-core"          % props.Refined4sVersion,
      "io.kevinlee" %% "refined4s-cats"          % props.Refined4sVersion,
      "io.kevinlee" %% "refined4s-extras-render" % props.Refined4sVersion,
    )

  lazy val refinedAll = List(
    "eu.timepit" %% "refined"      % props.RefinedVersion,
    "eu.timepit" %% "refined-cats" % props.RefinedVersion,
  )

  lazy val cats       = "org.typelevel" %% "cats-core"   % props.CatsVersion
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % props.CatsEffectVersion

  lazy val kittens = "org.typelevel" %% "kittens" % props.KittensVersion

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
    "io.kevinlee" %% "extras-string" % props.ExtrasVersion,
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

  lazy val declineAll = List(
    "com.monovore" %% "decline"        % props.DeclineVersion,
    "com.monovore" %% "decline-effect" % props.DeclineVersion,
  )

  lazy val log4JToSlf4J = "org.apache.logging.log4j" % "log4j-to-slf4j" % props.Log4JToSlf4JVersion
}
