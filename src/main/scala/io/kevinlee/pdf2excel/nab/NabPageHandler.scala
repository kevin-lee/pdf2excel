package io.kevinlee.pdf2excel.nab

import cats.parse.{Parser => P}
import cats.syntax.all._
import com.github.nscala_time.time.Imports.LocalDate
import io.kevinlee.parsers.Parsers.{alphabets, digits, monetaryNumbers, spaces, stringChars}
import io.kevinlee.pdf2excel.{Header, PageHandler, Transaction, TransactionDoc}

/** @author Kevin Lee
  * @since 2018-09-30
  */
case object NabPageHandler extends PageHandler[TransactionDoc] {

  val transactionStart: String = "Transaction details"

  def buildHeader(header: Seq[String]): Header = {
    val dateProcessed     = s"${header(0)} ${header(1)}"
    val dateOfTransaction = s"${header(2)} ${header(3)}"
    val rest              = header(5).split("[\\s]+")
//    val cardNo = s"${header(4)} ${rest(0)}"
    val details           = rest(1)
    val amount            = s"${rest(2)} ${rest(3)}"
    Header(dateProcessed, dateOfTransaction, details, amount)
  }

  def apply(page: Seq[String]): Option[TransactionDoc] = {
    val lines = page.dropWhile(line => line =!= transactionStart)
    if (lines.isEmpty) {
      None
    } else {
      val date   = (digits.rep.string ~ (P.char('/') *> digits.rep.string) ~ (P.char('/') *> digits.rep.string))
        .map {
          case ((day, month), year) =>
            LocalDate.parse(s"${(LocalDate.now().getYear / 100).toString}$year-$month-$day")
        }
      val lineP  =
        date ~ (spaces.rep *> date) ~ (spaces.rep *> (alphabets ~ digits.rep).string) ~ (spaces.rep *> stringChars.string) <* P.end
      val header = buildHeader(lines.slice(1, 7))

      val content =
        lines
          .drop(7)
          .foldLeft(Vector.empty[Transaction]) { (acc, x) =>
            lineP.parse(x.trim) match {
              case Right((_, (((dateProcessed, dateOfTransaction), cardNo), detailsAndAmount))) =>
                val splitted = detailsAndAmount.split("[\\s]+")
                val last     = splitted.last
                monetaryNumbers.parse(last) match {
                  case Right((_, price)) =>
                    acc :+ Transaction(dateProcessed, dateOfTransaction, splitted.init.mkString(" "), price)
                  case _ =>
                    acc
                }
              case _ =>
                acc
            }
          }
          .toList
      Some(TransactionDoc(header, content))
    }
  }
}
