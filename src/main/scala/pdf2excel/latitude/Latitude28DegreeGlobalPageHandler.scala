package pdf2excel.latitude

import cats.parse.Parser as P
import cats.parse.Parser.Error as ParserError
import cats.syntax.all.*
import com.github.nscala_time.time.Imports.LocalDate
import kevinlee.parsers.Parsers.*
import pdf2excel.{Header, PageHandler, Transaction, TransactionDoc}

import scala.annotation.tailrec

/** @author Kevin Lee
  * @since 2024-10-15
  */
object Latitude28DegreeGlobalPageHandler extends PageHandler[TransactionDoc] {

  private val transactionStart: String = "Your transactions"
  @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
  private val headers: List[String]    =
    List("Date", "Card", "Description", "Debits", "Credits").map(_.toLowerCase)
  private val lastLines: List[String]  = List("Closing balance")
  private val endMessage               =
    "Please review your transactions and contact us immediately"

  def buildHeader(header: String): Header = {
    val headerColumns = header.split("[\\s]+")
    val date          = headerColumns(0)
    val details       = headerColumns(2)
    Header(date, date, details, "amount")
  }

  @tailrec
  private def findTransactionStart(page: List[String]): List[String] = {
    val droppedLines1 = page.dropWhile(line => line =!= transactionStart)
    val lines         = if (droppedLines1.lengthIs < 2) {
      @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
      val (_, transactionLists) =
        page.span(line =>
          line
            .split("[\\s]", -1)
            .map(_.toLowerCase)
            .toList =!= headers
        )
      transactionLists
    } else {
      droppedLines1.drop(1)
    }
    if (lines.lengthIs < 2) {
      List.empty[String]
    } else {
      val found = lines.headOption.exists(_.trim.startsWith("Date"))
      if (found) lines else findTransactionStart(lines)
    }
  }

  @SuppressWarnings(
    Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any", "org.wartremover.warts.ListAppend")
  )
  def apply(page: List[String]): Option[TransactionDoc] = {
    val lines = findTransactionStart(page)
    if (lines.isEmpty) {
      None
    } else {
      val date = (digits.rep.string ~ (P.char('/') *> digits.rep.string) ~ (P.char('/') *> digits.rep.string))
        .map {
          case ((day, month), year) =>
            LocalDate.parse(s"$year-$month-$day")
        }

      val lineP  = date ~ (spaces.rep *> P.anyChar.rep(1).string) <* P.end
      val header = lines.headOption.map(buildHeader).getOrElse(Header("", "", "", ""))

      @tailrec
      def collect(lines: List[String], acc: List[String]): List[String] = lines match {
        case Nil =>
          acc
        case x :: xs =>
          val line = x // Don't trim!!!
          println(
            show"""line: $line
                  | [2]: ${xs.take(2)}
                  | [5]: ${xs.take(5)}
                  |""".stripMargin
          )
          if (lastLines.exists(line.contains) || line.contains(endMessage)) {
            acc
          } else {
            val (line1, line2) = (xs.headOption, xs.drop(1).headOption)
            if (
              line1.exists(l => lastLines.exists(l.contains)) && (
                line1.exists(_.contains(endMessage)) || line2.exists(_.contains(endMessage))
              )
            ) {
              acc :+ line
            } else {
              if (isSuccess(lineP.parse(line))) {
                val firstFive          = xs.take(5)
                val (beforeNext, next) = firstFive.span(l => isFailure(lineP.parse(l)))
                if (beforeNext.isEmpty) collect(xs, acc :+ line)
                else {
                  println(
                    show"""beforeNext: $beforeNext
                          |      next: $next
                          |""".stripMargin
                  )

                  val validBeforeNext = beforeNext.takeWhile { x =>
                    val y = x
                    println(
                      s"""y: $y
                         |""".stripMargin
                    )
                    !lastLines.exists(y.contains) && !y.contains(endMessage)
                  }
                  if (
                    lastLines.exists(lastWords => beforeNext.exists(_.contains(lastWords))) &&
                    (
                      beforeNext.exists(_.contains(endMessage)) ||
                        next.exists(_.contains(endMessage))
                    )
                  ) {
                    acc :+ (line +: validBeforeNext).mkString(" ")
                  } else {
                    collect(next ++ xs.drop(5), acc :+ (line +: validBeforeNext).mkString(" "))
                  }
                }
              } else {
                collect(xs, acc)
              }
            }
          }

      }

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def processLine(lines: List[String]): List[Transaction] = lines.flatMap { line =>
        lineP.parse(line) match {
          case Right((_, (d, a))) =>
            val words          = a.split("[\\s]", -1)
            println(s""">>> words:
                |${words.mkString(", ")}
                |>>>
                |""".stripMargin)

            val (details, Array(amount)) =
              /* Drop Card (number) and split to get the amount at the last column. */
              words.drop(1).splitAt(words.length - 2)
            println(s"""           a: $a
                       |       words: ${words.mkString(", ")}
                       |     details: ${details.mkString(", ")}
                       |      amount: $amount
                       |""".stripMargin)
            val filteredAmount = amount.trim.replace(",", "").stripPrefix("$")
            Vector(
              Transaction(
                d,
                d,
                details.mkString(" "),
                BigDecimal(filteredAmount)
              )
            )
          case Left(ParserError(_, _) | _: ParserError) =>
            Vector.empty[Transaction]
        }
      }

      val collected = collect(lines.drop(1), List.empty[String])
      println(
        s""">>> collected:
           |${collected.mkString("\n")}
           |>>>
           |""".stripMargin
      )
      val content   = processLine(collected)

      TransactionDoc(header, content).some
    }
  }

  override val getPostProcess: Option[TransactionDoc => TransactionDoc] = { (transactionDoc: TransactionDoc) =>

    @scala.annotation.tailrec
    def filterOutInternalTransactionFeeRebate(
      transactions: Vector[Transaction],
      acc: Vector[Transaction]
    ): Vector[Transaction] = {
      val firstTwo = transactions.take(2).toList
      val rest     = transactions.drop(2)
      firstTwo match {
        case trans1 :: trans2 :: Nil =>
          if (trans1.details.trim === "Intl Transaction Fee" && trans2.details.trim === "Intl Transaction Fee Rebate") {
            if (trans1.amount.abs - trans2.amount.abs === BigDecimal(0)) {
              filterOutInternalTransactionFeeRebate(rest, acc)
            } else {
              filterOutInternalTransactionFeeRebate(rest, acc :+ trans1 :+ trans2)
            }
          } else {
            filterOutInternalTransactionFeeRebate(trans2 +: rest, acc :+ trans1)
          }

        case _ :: _ :: _ =>
          sys.error(
            "This is a bug in filterOutInternalTransactionFeeRebate!!! filtering should never reach this pattern."
          )

        case trans :: Nil =>
          acc :+ trans

        case Nil =>
          acc
      }

    }
    transactionDoc.copy(content =
      filterOutInternalTransactionFeeRebate(transactionDoc.content.toVector, Vector.empty[Transaction])
    )
  }.some

}
