package io.kevinlee.pdf2excel

import java.io.File

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory

import info.folone.scala.poi.{NumericCell, Row, Sheet, StringCell, Workbook}

import io.kevinlee.skala.strings.StringGlues._
import io.kevinlee.pdf2excel.ing.IngPageHandler

import net.ceedubs.ficus.Ficus._

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

import scalaz._, Scalaz._


object Pdf2Excel {

  final case class FromTo(from: Int, to: Int)

  def handlePages(
    f: Seq[String] => Option[TransactionDoc],
    pages: List[Seq[String]]
  ): Option[TransactionDoc] = {
    val sequence: Option[List[TransactionDoc]] = pages.map(f).filter(_.isDefined).sequence
    val maybeDoc: Option[TransactionDoc] =
      sequence.flatMap(_.reduceLeftOption((x, y) => x.copy(content = x.content ++ y.content)))
    maybeDoc
  }


  def convertToText(
    inputFile: File,
    fromTo: Option[FromTo]
  ): List[List[String]] = {

    import io.kevinlee.skala.util.TryWith.SideEffect.tryWith

    tryWith(PDDocument.load(inputFile)) { pdf =>
      val (startPage, endPage) =
        fromTo.fold((1, pdf.getNumberOfPages))(fromTo => (fromTo.from, fromTo.to))

      (for {
        page <- startPage to endPage
        onePage = convertOnePageToText(pdf, page)
      } yield onePage).map(_.split("[\\n]+").toList).toList
    }
  }

  def convertOnePageToText(pdf: PDDocument, page: Int): String = {
      val stripper = new PDFTextStripper
      stripper.setStartPage(page)
      stripper.setEndPage(page)
      stripper.getText(pdf).trim
  }

  private def writeExcel(transactionDoc: TransactionDoc, outputPath: String): Unit = {
    val headerPosition = 2
    val rowPositionOffet = headerPosition + 1
    val sheetOne = Workbook {
      Set(
        Sheet(s"Credit Card ${LocalDateTime.now().toString("yyyy-MM")}") {
          Set(
            Row(headerPosition) {
              Header.toSeq(transactionDoc.header).zipWithIndex.map { case (x, i) =>
                StringCell(i, x)
              }.toSet
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
    sheetOne.safeToFile(outputPath)
      .fold(ex => sys.error(ex.getMessage), identity)
      .unsafePerformIO
  }

  private val FilenameAndExt = "(.+)[\\.]([^\\.]+)$".r
  private def replaceFileExtension(filename: String, newExt: String): String =
    filename match {
      case FilenameAndExt(name, _) =>
        s"$name.$newExt"
      case _ =>
        s"$filename.$newExt"
    }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val pdfConfig = config.getConfig("pdf")
    val inputFilename = pdfConfig.as[String]("path")
    val outputDir = config.as[String]("excel.path")
    val inputFile = new File(inputFilename)
    val outputPath = outputDir / replaceFileExtension(inputFile.getName, "xls")

    val fromTo: Option[FromTo] = for {
        from <- pdfConfig.getAs[Int]("from")
        to <- pdfConfig.getAs[Int]("to")
        theFromTo = FromTo(from, to)
      } yield theFromTo

    val pages = convertToText(inputFile, fromTo)


    // TODO: get it from parameter or config file
//    val maybeDoc: Option[TransactionDoc] = handlePages(PageHandler1, pages)
//    val maybeDoc: Option[TransactionDoc] = handlePages(CbaPageHandler2, pages)
    val maybeDoc: Option[TransactionDoc] = handlePages(IngPageHandler, pages)

    println(
      s"""maybeDoc: $maybeDoc
         |""".stripMargin
    )
    println(s"Write to $outputPath")
    maybeDoc.foreach(transactionDoc => writeExcel(transactionDoc, outputPath))
  }

}
