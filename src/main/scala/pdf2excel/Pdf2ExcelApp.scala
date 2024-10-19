package pdf2excel

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import effectie.instances.ce3.fx.ioFx
import effectie.resource.{Ce3ResourceMaker, ResourceMaker}
import effectie.syntax.all.*
import pdf2excel.Args.{Excel, Pdf}
import pdf2excel.Args.Pdf.FromTo
import refined4s.compat.RefinedCompatAllTypes

import java.nio.file.Path

object Pdf2ExcelApp
    extends CommandIOApp(
      name = "pdf2excel",
      header = "PDF to Excel Converter",
      version = "0.1.0"
    )
    with RefinedCompatAllTypes {

  val pdfPathOpt: Opts[Pdf.Path] = Opts
    .option[Path](long = "pdf-path", short = "p", metavar = "path-to-pdf-file", help = "The path to the PDF file")
    .mapValidated { path =>
      val file = path.toFile
      if (file.exists)
        file.validNel[String]
      else
        s"The PDF doesn't exist at ${file.getCanonicalPath}".invalidNel
    }
    .map(Args.Pdf.Path(_))

  val pdfFromOpt: Opts[Option[FromTo.From]] = Opts
    .option[Int](long = "from", short = "f", metavar = "page-number", help = "The first page to start reading")
    .mapValidated { from =>
      PosInt.from(from).toValidatedNel
    }
    .map(Args.Pdf.FromTo.From(_))
    .orNone

  val pdfToOpt: Opts[Option[FromTo.To]] = Opts
    .option[Int](long = "to", short = "t", metavar = "page-number", help = "The last page to end reading")
    .mapValidated { to =>
      PosInt.from(to).toValidatedNel
    }
    .map(Args.Pdf.FromTo.To(_))
    .orNone

  val excelPathOpt: Opts[Excel.Path] = Opts
    .option[Path](
      long = "excel-path",
      short = "e",
      metavar = "path-to-folder",
      help = "The path to the folder to create the result Excel file"
    )
    .mapValidated { path =>
      val file = path.toFile
      if (file.exists)
        file.validNel[String]
      else
        s"The folder for the result excel file doesn't exist at ${file.getCanonicalPath}".invalidNel
    }
    .map(Args.Excel.Path(_))

  override def main: Opts[IO[ExitCode]] = {
    (pdfPathOpt, pdfFromOpt, pdfToOpt, excelPathOpt).mapN { (pdfPath, from, to, excelPath) =>
      (for {
        fromTo <- (from, to) match {
                    case (Some(fromValue), Some(toValue)) =>
                      if (fromValue.value.value <= toValue.value.value) {
                        IO.pure(Args.Pdf.FromTo(from, to))
                      } else {
                        IO.raiseError(
                          new IllegalArgumentException(
                            show"Invalid from and to. from value should be less than or equal to to. [from: $from, to: $to]"
                          )
                        )
                      }

                    case (from, to) =>
                      IO.pure(Args.Pdf.FromTo(from, to))
                  }

        _ <- runApp(Args.Pdf(pdfPath, fromTo), Args.Excel(excelPath))
      } yield ExitCode.Success).handleErrorWith(err => IO.println(err) *> ExitCode.Error.pure[IO])

    }
  }

  private def runApp(pdf: Args.Pdf, excel: Args.Excel): IO[Unit] = {
    implicit val resourceMaker: ResourceMaker[IO] = Ce3ResourceMaker.maker[IO]
    for {
      outputDir  <- pureOf(excel.path)
      inputFile  <- pureOf(pdf.path)
      outputPath <- pureOrError(
                      s"${outputDir.value.getCanonicalPath}/${replaceFileExtension(inputFile.value.getName, "xls")}"
                    )

      pdf2Excel = Pdf2Excel[IO]
      _ <- pdf2Excel.runF(inputFile.value, outputPath, pdf.fromTo)
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
