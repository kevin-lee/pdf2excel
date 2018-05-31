package io.kevinlee.pdf2excel


import java.io.File

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import fastparse.all._
import fastparse.core.Parsed.Success
import info.folone.scala.poi.{NumericCell, Row, Sheet, StringCell, Workbook}
import io.kevinlee.parsers.Parsers.{alphabets, digits, numbers, spaces, stringChars}
import io.kevinlee.skala.strings.StringGlues._
import net.ceedubs.ficus.Ficus._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import scalaz._
import Scalaz._

object Pdf2Excel {

  final case class FromTo(from: Int, to: Int)

  final case class Header(dateProcessed: String,
                          dateOfTransaction: String,
                          cardNo: String,
                          details: String,
                          amount: String) {
    def toSeq: Seq[String] = Vector(dateOfTransaction, details, amount)
  }

  final case class Transaction(dateProcessed: LocalDate,
                               dateOfTransaction: LocalDate,
                               cardNo: String,
                               details: String,
                               amount: BigDecimal)

  final case class TransactionDoc(header: Header, content: List[Transaction]) {
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    lazy override val toString: String =
      s"""TransactionDoc(
         |  $header,
         |  ${"-" * header.toString.length}
         |  ${content.mkString("", "\n  ", "\n")})
         |""".stripMargin
  }

  def handlePage(transactionStart: String, page: Seq[String]): Option[TransactionDoc] = {
    val lines = page.dropWhile(line => line =/= transactionStart)
    if (lines.isEmpty) {
      None
    } else {
      val date = P(digits.rep.! ~ "/" ~ digits.rep.! ~ "/" ~ digits.rep.!).map { case (day, month, year) =>
          LocalDate.parse(s"${LocalDate.now().getYear / 100}$year-$month-$day")
      }
      val lineP = date ~ spaces.rep ~ date ~ spaces.rep ~ (alphabets ~ digits.rep).! ~ spaces.rep ~ stringChars.! ~ End
      val header = buildHeader(lines.slice(1, 7))

      val content =
        lines.drop(7)
             .foldLeft(Vector.empty[Transaction]) { (acc, x) =>
               lineP.parse(x.trim) match {
                 case Success((dateProcessed, dateOfTransaction, cardNo, detailsAndAmount), _) =>
                   val splitted = detailsAndAmount.split("[\\s]+")
                   val last = splitted.last
                   numbers.parse(last) match {
                     case Success(price, _) =>
                       acc :+ Transaction(dateProcessed, dateOfTransaction, cardNo, splitted.init.mkString(" "), price)
                     case _ =>
                       acc
                   }
                 case _ =>
                   acc
               }
             }.toList
      Some(TransactionDoc(header, content))
    }
  }

  def buildHeader(header: Seq[String]): Header = {
    val dateProcessed = s"${header(0)} ${header(1)}"
    val dateOfTransaction = s"${header(2)} ${header(3)}"
    val rest = header(5).split("[\\s]+")
    val cardNo = s"${header(4)} ${rest(0)}"
    val details = rest(1)
    val amount = s"${rest(2)} ${rest(3)}"
    Header(dateProcessed, dateOfTransaction, cardNo, details, amount)
  }

  def convertToText(inputFile: File,
                    fromTo: Option[FromTo]): List[List[String]] = {

    import io.kevinlee.skala.util.TryWith.SideEffect.tryWith

    tryWith(PDDocument.load(inputFile)) { pdf =>
      val (startPage, endPage) =
        fromTo.fold((1, pdf.getNumberOfPages)){ fromTo =>
          (fromTo.from, fromTo.to)
        }

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
              transactionDoc.header.toSeq.zipWithIndex.map { case (x, i) =>
                StringCell(i, x)
              }.toSet
            }
          ) ++ (for {
            (trans, i) <- transactionDoc.content.zipWithIndex
            row = Row(i + rowPositionOffet) {
              val _@Transaction(_, dateOfTransaction, _, details, amount) = trans
              Set(
                StringCell(0, dateOfTransaction.toString),
                StringCell(1, details),
                NumericCell(2, amount.toDouble)
              )
            }
          } yield row).toSet
        }
      )
    }
    sheetOne.safeToFile(outputPath).fold(ex ⇒ sys.error(ex.getMessage), identity).unsafePerformIO
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
    val transactionStart = pdfConfig.as[String]("transaction.start")
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
    val sequence: Option[List[TransactionDoc]] = pages.map(handlePage(transactionStart, _)).filter(_.isDefined).sequence
    val maybeDoc: Option[TransactionDoc] =
      sequence.flatMap(_.reduceLeftOption((x, y) => x.copy(content = x.content ++ y.content)))

    println(
      s"""maybeDoc: $maybeDoc
         |""".stripMargin
    )
    println(s"Write to $outputPath")
    maybeDoc.foreach(transactionDoc => writeExcel(transactionDoc, outputPath))
  }

}
