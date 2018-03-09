package io.kevinlee.pdf2excel


import java.io.File

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import fastparse.all._
import fastparse.core.Parsed.Success
import info.folone.scala.poi.{NumericCell, Row, Sheet, StringCell, Workbook}
import io.kevinlee.parsers.Parsers.{alphabets, digits, numbers, spaces, stringChars}
import net.ceedubs.ficus.Ficus._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import scalaz.Scalaz._

object Pdf2Excel {

  case class FromTo(from: Int, to: Int)

  case class Header(dateProcessed: String,
                    dateOfTransaction: String,
                    cardNo: String,
                    details: String,
                    amount: String) {
    def toSeq: Seq[String] = Vector(dateOfTransaction, details, amount)
  }
  case class Transaction(dateProcessed: LocalDate,
                         dateOfTransaction: LocalDate,
                         cardNo: String,
                         details: String,
                         amount: BigDecimal) {
    def toSeq = Vector(dateOfTransaction, details, amount)
  }
  case class TransactionDoc(header: Header, content: List[Transaction]) {
    lazy override val toString: String =
      s"""TransactionDoc(
         |  $header,
         |  ${"-" * header.toString.length}
         |  ${content.mkString("", "\n  ", "\n")})
         |""".stripMargin
  }

  def handlePage(transactionStart: String, page: Seq[String]): Option[TransactionDoc] = {
    val lines = page.dropWhile(line => line != transactionStart)
    if (lines.isEmpty) {
      None
    } else {
      val date = P(digits.rep.! ~ "/" ~ digits.rep.! ~ "/" ~ digits.rep.!).map { case (date, month, year) =>
          LocalDate.parse(s"${LocalDate.now().getYear / 100}$year-$month-$date")
      }
      val lineP = date ~ spaces.rep ~ date ~ spaces.rep ~ (alphabets ~ digits.rep).! ~ spaces.rep ~ stringChars.! ~ End
      val header = buildHeader(lines.tail.take(6))


      val content =
        lines.drop(7)
        .foldLeft(Vector.empty[Transaction])((acc, x) => {
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
        }).toList
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

  def convertToText(filename: String,
                    fromTo: Option[FromTo]): List[List[String]] = {

    import io.kevinlee.skala.util.TryWith.SideEffect.tryWith

    tryWith(PDDocument.load(new File(filename))) { pdf =>
      val (startPage, endPage) = fromTo.fold((1, pdf.getNumberOfPages)){ fromTo => (fromTo.from, fromTo.to) }

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

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val pdfConfig = config.getConfig("pdf")
    val transactionStart = pdfConfig.as[String]("transaction.start")
    val inputFile = pdfConfig.as[String]("path")
    val outputFile = config.as[String]("excel.path")

    val fromTo: Option[FromTo] = for {
        from <- pdfConfig.getAs[Int]("from")
        to <- pdfConfig.getAs[Int]("to")
        theFromTo = FromTo(from, to)
      } yield theFromTo

    val pages = convertToText(inputFile, fromTo)
    val sequence: Option[List[TransactionDoc]] = pages.map(handlePage(transactionStart, _)).filter(_.isDefined).sequence
    val maybeDoc: Option[TransactionDoc] = sequence.map(_.reduce((x, y) => x.copy(content = x.content ++ y.content)))

    println(
      s"""maybeDoc: $maybeDoc
         |""".stripMargin)
    maybeDoc.foreach { statementDoc =>
      val headerPosition = 2
      val rowPositionOffet = headerPosition + 1
      val sheetOne = Workbook {
        Set(
          Sheet(s"Credit Card ${LocalDateTime.now().toString("yyyy-MM")}") {
            Set(
              Row(headerPosition) {
                statementDoc.header.toSeq.zipWithIndex.map { case (x, i) => StringCell(i, x) }.toSet
              }
            ) ++ (for {
              (trans, i) <- statementDoc.content.zipWithIndex
              row = Row(i + rowPositionOffet) {
                (for {
                  (value, i) <- trans.toSeq.zipWithIndex
                  cell = value match {
                    case amount: BigDecimal => NumericCell(i, amount.toDouble)
                    case _ =>
                      StringCell(i, value.toString)
                  }
                } yield cell).toSet
              }
            } yield row).toSet
          }
        )
      }
      sheetOne.safeToFile(outputFile).fold(ex ⇒ throw ex, identity).unsafePerformIO
    }
  }

}
