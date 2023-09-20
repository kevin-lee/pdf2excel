package pdf2excel

import cats.effect.{IO, IOApp}
import cats.syntax.all._
import effectie.instances.ce2.fx._
import effectie.resource.{Ce2ResourceMaker, ResourceMaker}
import extras.cats.syntax.all._
import pdf2excel.config.Pdf2ExcelConfig

import java.io.File

object Pdf2ExcelApp extends IOApp.Simple {

  override def run: IO[Unit] = {
    implicit val resourceMaker: ResourceMaker[IO] = Ce2ResourceMaker.maker[IO]
    for {
      config <- Pdf2ExcelConfig.load[IO].innerLeftMap(err => new RuntimeException(err.prettyPrint(2))).rethrow

      inputFilename = config.pdf.path
      outputDir     = config.excel.path
      inputFile     = new File(inputFilename)
      outputPath    = s"$outputDir/${replaceFileExtension(inputFile.getName, "xls")}"

      pdf2Excel = Pdf2Excel[IO]
      _ <- pdf2Excel.runF(config, inputFile, outputPath)
    } yield ()
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
