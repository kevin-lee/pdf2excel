package io.kevinlee.pdf2excel.cba

import cats.parse.Parser.{Error => ParserError}
import cats.parse.{Parser => P}
import cats.syntax.all._
import com.github.nscala_time.time.Imports.LocalDate
import io.kevinlee.parsers.Parsers._
import io.kevinlee.pdf2excel.{Header, PageHandler, Transaction, TransactionDoc}

import scala.annotation.tailrec

/** @author Kevin Lee
  * @since 2018-09-30
  */
case object CbaPageHandler extends PageHandler[TransactionDoc] {

  val transactionStart: String = "Transactions"

  def buildHeader(header: String): Header = {
    val headerColumns = header.split("[\\s]+")
    val date          = headerColumns(0)
    val details       = s"${headerColumns(1)} ${headerColumns(2)}"
    val _             = s"${headerColumns(3)} ${headerColumns(4)}" // card number
    val amount        = s"${headerColumns(5)} ${headerColumns(6)}"
    Header(date, date, details, amount)
  }

  @tailrec
  def findTransactionStart(page: Seq[String]): Seq[String] = {
    val droppedLines1 = page.dropWhile(line => line =!= transactionStart)
    if (droppedLines1.lengthIs < 2) {
      Vector.empty[String]
    } else {
      val lines = droppedLines1.drop(1)
      val found = lines.headOption.exists(_.trim.startsWith("Date"))
      if (found) lines else findTransactionStart(lines)
    }
  }

  private def decideYear(month: Int): Int = {
    val now  = LocalDate.now()
    val year = now.getYear
    if (month === 12 && now.getMonthOfYear =!= 12)
      year - 1
    else
      year
  }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def apply(page: Seq[String]): Option[TransactionDoc] = {
    val lines = findTransactionStart(page)
    if (lines.isEmpty) {
      None
    } else {
      val months: Map[String, Int] =
        List("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
          .zipWithIndex
          .map { case (x, i) => (x, i + 1) }
          .toMap

      val monthsP = P.stringIn(months.keys)

      val date = (digits.rep.string ~ (spaces.rep *> monthsP.string)).map {
        case (day, month) =>
          val monthValue = months(month)
          LocalDate.parse(s"${decideYear(monthValue).toString}-${monthValue.toString}-${day}")
      }

      val lineP  =
        date ~ (spaces.rep *> P.anyChar.rep(1).string) <* P.end
      val header = lines.headOption.map(buildHeader).getOrElse(Header("", "", "", ""))

      @tailrec
      def processLine(lines: Seq[String], acc: Seq[Transaction]): Seq[Transaction] = lines match {
        case Nil =>
          acc
        case x :: xs =>
          val line = x.trim
          lineP.parse(line) match {
            case Right((_, (d, a))) =>
              val (twoMoreLines, rest) = xs.splitAt(2)
              if (
                twoMoreLines.length === 2 && twoMoreLines
                  .drop(1)
                  .headOption
                  .fold(false)(_.trim.startsWith("Mastercard"))
              ) {
                processLine(s"$line ${twoMoreLines.map(_.trim).mkString(" ")}" :: rest, acc)
              } else {
                val words                          = a.split("[\\s]+").map(_.trim)
                val (details, Array(card, amount)) = words.splitAt(words.length - 2)
                val filteredAmount                 = amount.replace(",", "")
                processLine(
                  xs,
                  acc :+ Transaction(
                    d,
                    d,
                    details.mkString(" "),
                    BigDecimal(
                      if (filteredAmount.endsWith("-"))
                        s"-${filteredAmount.dropRight(1)}"
                      else
                        filteredAmount
                    )
                  )
                )
              }
            case Left(ParserError(_, _) | _: ParserError) =>
              processLine(xs, acc)
          }

      }

      val content = processLine(lines.drop(1), Vector.empty)
      Some(TransactionDoc(header, content))
    }
  }
}
