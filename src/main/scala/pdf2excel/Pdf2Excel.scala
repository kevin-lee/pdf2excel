package pdf2excel

import cats.*
import cats.syntax.all.*
import com.github.nscala_time.time.Imports.*
import effectie.core.*
import effectie.resource.ResourceMaker
import effectie.syntax.all.*
import info.folone.scala.poi.{NumericCell, Row, Sheet, StringCell, Workbook}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.joda.time.format.ISODateTimeFormat
import refined4s.compat.RefinedCompatAllTypes

import java.io.File

trait Pdf2Excel[F[*]] {
  def runF(inputFile: File, outputPath: String, fromTo: Args.Pdf.FromTo): F[Unit]
}

object Pdf2Excel extends RefinedCompatAllTypes {

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
      fromTo: Args.Pdf.FromTo,
    ): F[List[List[String]]] =
      ResourceMaker[F]
        .forAutoCloseable(effectOf[F](PDDocument.load(inputFile)))
        .use { pdf =>
          val (startPage, endPage) =
            (fromTo.from.fold(PosInt(1))(_.value), fromTo.to.fold(PosInt.unsafeFrom(pdf.getNumberOfPages))(_.value))

          effectOf[F](
            (for {
              page <- startPage.value to endPage.value
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

    def runF(inputFile: File, outputPath: String, fromTo: Args.Pdf.FromTo): F[Unit] =
      for {
        pages    <- convertToText(inputFile, fromTo)
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
