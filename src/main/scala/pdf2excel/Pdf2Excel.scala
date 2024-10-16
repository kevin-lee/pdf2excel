package pdf2excel

import cats.*
import cats.syntax.all.*
import com.github.nscala_time.time.Imports.*
import effectie.core.*
import effectie.resource.ResourceMaker
import effectie.syntax.all.*
import info.folone.scala.poi.{NumericCell, Row, Sheet, StringCell, Workbook}
import pdf2excel.config.Pdf2ExcelConfig
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.joda.time.format.ISODateTimeFormat
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

import java.io.File

trait Pdf2Excel[F[*]] {
  def runF(pdfConfig: Pdf2ExcelConfig, inputFile: File, outputPath: String): F[Unit]
}

object Pdf2Excel {

  final case class FromTo(from: Int, to: Int)
  object FromTo {
    implicit val fromToConfigReader: ConfigReader[FromTo] =
      ConfigReader.fromString { s =>
        s.split("\\s+-\\s+").toList match {
          case from :: to :: Nil =>
            (
              from.toIntOption.filter(_ > 0).toRightNec("from must be positive Int"),
              to.toIntOption.filter(_ > 0).toRightNec("to must be positive Int")
            )
              .parFlatMapN((from, to) =>
                if (from <= to)
                  FromTo(from, to).rightNec[String]
                else
                  "`from` value must be less than or equal to `to` value".leftNec[FromTo]
              )
              .leftMap(err => CannotConvert(s, "FromTo", err.mkString_("[", ", ", "]")))

          case _ =>
            CannotConvert(
              s,
              "FromTo",
              "fromTo value must be two positive Int values separated by - (e.g. 1 to 10, 1 - 1, etc.)"
            ).asLeft
        }
      }

  }

  def apply[F[*]: Fx: Monad: ResourceMaker]: Pdf2Excel[F] = new Pdf2ExcelF[F]

  final private class Pdf2ExcelF[F[*]: Fx: Monad: ResourceMaker] extends Pdf2Excel[F] {

    def handlePages(
      f: List[String] => Option[TransactionDoc],
      postProcess: Option[TransactionDoc => TransactionDoc],
      pages: List[List[String]]
    ): F[Option[TransactionDoc]] = {
      val sequence: Option[List[TransactionDoc]] = pages.map(f).filter(_.isDefined).sequence

      val maybeDoc: Option[TransactionDoc] =
        sequence.flatMap(_.reduceLeftOption((x, y) => x.copy(content = x.content ++ y.content)))

      effectOf[F](postProcess.fold(maybeDoc)(maybeDoc.map))
    }

    def convertToText(
      inputFile: File,
      fromTo: Option[FromTo]
    ): F[List[List[String]]] =
      ResourceMaker[F]
        .forAutoCloseable(effectOf[F](PDDocument.load(inputFile)))
        .use { pdf =>
          val (startPage, endPage) =
            fromTo.fold((1, pdf.getNumberOfPages))(fromTo => (fromTo.from, fromTo.to))

          effectOf[F](
            (for {
              page <- startPage to endPage
              onePage = convertOnePageToText(pdf, page)
            } yield onePage).map(_.split("[\\n]+").toList).toList
          )
        }

    def convertOnePageToText(pdf: PDDocument, page: Int): String = {
      val stripper = new PDFTextStripper
      stripper.setStartPage(page)
      stripper.setEndPage(page)
      stripper.getText(pdf).trim
    }

    private def writeExcel(transactionDoc: TransactionDoc, outputPath: String): Unit = {
      val headerPosition   = 2
      val rowPositionOffet = headerPosition + 1
      val sheetOne         = Workbook {
        Set(
          Sheet(s"Credit Card ${LocalDateTime.now().toString("yyyy-MM")}") {
            Set(
              Row(headerPosition) {
                Header
                  .toSeq(transactionDoc.header)
                  .zipWithIndex
                  .map {
                    case (x, i) =>
                      StringCell(i, x)
                  }
                  .toSet
              }
            ) ++ (for {
              (trans, i) <- transactionDoc.content.zipWithIndex
              row = Row(i + rowPositionOffet) {
                      trans match {
                        case Transaction(_, dateOfTransaction, details, amount) =>
                          Set(
                            StringCell(0, ISODateTimeFormat.date().print(dateOfTransaction)),
                            StringCell(1, details),
                            NumericCell(2, amount.toDouble),
                          )
                      }
                    }
            } yield row).toSet
          }
        )
      }
      sheetOne
        .safeToFile(outputPath)
        .fold(ex => sys.error(ex.getMessage), identity)
        .unsafePerformIO()
    }

    def runF(pdfConfig: Pdf2ExcelConfig, inputFile: File, outputPath: String): F[Unit] = {

//      val fromTo: Option[FromTo] = for {
//        from <- pdfConfig.getAs[Int]("from")
//        to   <- pdfConfig.getAs[Int]("to")
//        theFromTo = FromTo(from, to)
//      } yield theFromTo

      for {
        pages    <- convertToText(inputFile, pdfConfig.pdf.fromTo)
        // TODO: get it from parameter or config file
        //    val maybeDoc: Option[TransactionDoc] = handlePages(PageHandler1, pages)
        //    val maybeDoc: Option[TransactionDoc] = handlePages(pdf2excel.cba.CbaPageHandler2, none[TransactionDoc => TransactionDoc], pages)
        maybeDoc <- handlePages(
                      pdf2excel.ing.IngPageHandler,
                      (pdf2excel.ing.IngPageHandler.postProcess _).some,
                      pages,
                    )
        _        <- maybeDoc match {
                      case Some(transactionDoc) =>
                        putStrLn(
                          s"""maybeDoc: ${transactionDoc.toString}
                               |""".stripMargin
                        ) *>
                          putStrLn(s"Write to $outputPath") *>
                          effectOf(writeExcel(transactionDoc, outputPath))

                      case None =>
                        errorOf(new RuntimeException(s"No document found at ${inputFile.getCanonicalPath}"))
                    }

      } yield ()

    }
  }
}
