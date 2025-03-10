package pdf2excel.cba

import cats.parse.Parser.{Error => ParserError}
import cats.parse.{Parser => P}
import cats.syntax.all._
import com.github.nscala_time.time.Imports.LocalDate
import kevinlee.parsers.Parsers._
import pdf2excel.{Header, PageHandler, Transaction, TransactionDoc}

import scala.annotation.tailrec

/** Since April 2019, this should be used instead of CbaPageHandler
  * @author Kevin Lee
  * @since 2019-04-27
  */
case object CbaPageHandler2 extends PageHandler[TransactionDoc] {

  private val transactionStart: String = "Transactions"
  private val lastLines: List[String]  = List("Interest charged on purchases", "Interest charged on cash advances")
  private val endMessage               =
    "Please check your transactions listed on this statement and report any discrepancy to the Bank before the payment due date. Mastercard is the registered trademark of Mastercard International Incorporated."

  def buildHeader(header: String): Header = {
    val headerColumns = header.split("[\\s]+")
    val date          = headerColumns(0)
    val details       = s"${headerColumns(1)} ${headerColumns(2)}"
    val amount        = s"${headerColumns(3)} ${headerColumns(4)}"
    Header(date, date, details, amount)
  }

  @tailrec
  def findTransactionStart(page: List[String]): List[String] = {
    val droppedLines1 = page.dropWhile(line => line =!= transactionStart)
    if (droppedLines1.lengthIs < 2) {
      List.empty[String]
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

  @SuppressWarnings(
    Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any", "org.wartremover.warts.ListAppend")
  )
  def apply(page: List[String]): Option[TransactionDoc] = {
    val lines = findTransactionStart(page)
    if (lines.isEmpty) {
      None
    } else {
      val months =
        List("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
          .zipWithIndex
          .map { case (x, i) => (x, i + 1) }
          .toMap

      val monthsP = P.stringIn(months.keys)

      val date = (digits.rep.string ~ (spaces.rep *> monthsP.string)).map {
        case (day, month) =>
          val monthValue = months(month)
          LocalDate.parse(s"${decideYear(monthValue).toString}-${monthValue.toString}-$day")
      }

      val lineP  =
        date ~ (spaces.rep *> P.anyChar.rep(1).string) <* P.end
      val header = lines.headOption.map(buildHeader).getOrElse(Header("", "", "", ""))

      @tailrec
      def collect(lines: List[String], acc: List[String]): List[String] = lines match {
        case Nil =>
          acc
        case x :: xs =>
          val line = x.trim
          if (lastLines.exists(line.contains) || line === endMessage) {
            acc
          } else if (xs.take(2) === lastLines) {
            acc :+ line
          } else {
            if (isSuccess(lineP.parse(line))) {
              val firstFive          = xs.take(5)
              val (beforeNext, next) = firstFive.span(l => isFailure(lineP.parse(l)))
              if (beforeNext.isEmpty)
                collect(xs, acc :+ line)
              else {
                val validBeforeNext = beforeNext.takeWhile { x =>
                  val y = x.trim
                  !lastLines.exists(y.contains) && y =!= endMessage
                }
                collect(next ++ xs.drop(5), acc :+ (line +: validBeforeNext.toVector).mkString(" "))
              }
            } else {
              collect(xs, acc)
            }
          }

      }

      def processLine(lines: List[String]): List[Transaction] = lines.flatMap { line =>
        lineP.parse(line) match {
          case Right((_, (d, a))) =>
            val words                    = a.split("[\\s]+").map(_.trim)
            val (details, Array(amount)) = words.splitAt(words.length - 1)
            val filteredAmount           = amount.replace(",", "")
            Vector(
              Transaction(
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
          case Left(ParserError(_, _) | _: ParserError) =>
            Vector.empty
        }
      }

      val collected = collect(lines.drop(1), List.empty)
      val content   = processLine(collected)
      Some(TransactionDoc(header, content))
    }
  }

}
