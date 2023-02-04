package io.kevinlee.pdf2excel

import cats._
import cats.syntax.all._
import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config
import effectie.core._
import effectie.resource.ResourceMaker
import effectie.syntax.all._
import info.folone.scala.poi.{NumericCell, Row, Sheet, StringCell, Workbook}
import net.ceedubs.ficus.Ficus._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

import java.io.File

trait Pdf2Excel[F[*]] {
  def runF(pdfConfig: Config, inputFile: File, outputPath: String): F[Unit]
}

object Pdf2Excel {

  final case class FromTo(from: Int, to: Int)

  def apply[F[*]: Fx: Monad: ResourceMaker: ConsoleEffect]: Pdf2Excel[F] = new Pdf2ExcelF[F]

  final private class Pdf2ExcelF[F[*]: Fx: Monad: ResourceMaker: ConsoleEffect] extends Pdf2Excel[F] {

    def handlePages(
      f: Seq[String] => Option[TransactionDoc],
      postProcess: Option[TransactionDoc => TransactionDoc],
      pages: List[Seq[String]]
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
                            StringCell(0, dateOfTransaction.toString),
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

    def runF(pdfConfig: Config, inputFile: File, outputPath: String): F[Unit] = {

      val fromTo: Option[FromTo] = for {
        from <- pdfConfig.getAs[Int]("from")
        to   <- pdfConfig.getAs[Int]("to")
        theFromTo = FromTo(from, to)
      } yield theFromTo

      for {
        pages    <- convertToText(inputFile, fromTo)
        // TODO: get it from parameter or config file
        //    val maybeDoc: Option[TransactionDoc] = handlePages(PageHandler1, pages)
        //    val maybeDoc: Option[TransactionDoc] = handlePages(io.kevinlee.pdf2excel.cba.CbaPageHandler2, none[TransactionDoc => TransactionDoc], pages)
        maybeDoc <- handlePages(
                      io.kevinlee.pdf2excel.ing.IngPageHandler,
                      (io.kevinlee.pdf2excel.ing.IngPageHandler.postProcess _).some,
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
