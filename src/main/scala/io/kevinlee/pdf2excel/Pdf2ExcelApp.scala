package io.kevinlee.pdf2excel

import cats.effect.{IO, IOApp}
import com.typesafe.config.ConfigFactory
import effectie.resource.{Ce2ResourceMaker, ResourceMaker}
import net.ceedubs.ficus.Ficus._

import java.io.File

object Pdf2ExcelApp extends IOApp.Simple {

  override def run: IO[Unit] = {

    val config        = ConfigFactory.load()
    val pdfConfig     = config.getConfig("pdf")
    val inputFilename = pdfConfig.as[String]("path")
    val outputDir     = config.as[String]("excel.path")
    val inputFile     = new File(inputFilename)
    val outputPath    = s"$outputDir/${replaceFileExtension(inputFile.getName, "xls")}"

    import effectie.instances.ce2.fx._
    import effectie.instances.console._

    implicit val resourceMaker: ResourceMaker[IO] = Ce2ResourceMaker.forAutoCloseable[IO]

    val pdf2Excel = Pdf2Excel[IO]
    pdf2Excel.runF(pdfConfig, inputFile, outputPath)
  }

  private val FilenameAndExt = "(.+)[\\.]([^\\.]+)$".r

  private def replaceFileExtension(filename: String, newExt: String): String =
    filename match {
      case FilenameAndExt(name, _) =>
        s"$name.$newExt"
      case _ =>
        s"$filename.$newExt"
    }

}
