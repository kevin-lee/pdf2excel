package io.kevinlee.pdf2excel.ing

import cats.syntax.all._
import com.github.nscala_time.time.Imports.LocalDate
import fastparse.all._
import io.kevinlee.parsers.Parsers._
import io.kevinlee.pdf2excel.{Header, PageHandler, Transaction, TransactionDoc}

import scala.annotation.tailrec

/**
 * @author Kevin Lee
 * @since 2020-10-19
 */
object IngPageHandler extends PageHandler[TransactionDoc] {

  private val transactionStart: String = "Transactions"
  private val headers: List[String] = List("Date", "Details", "Money", "out", "Money", "in", "Balance").map(_.toLowerCase)
  private val lastLines: List[String] = List("Closing balance")
  private val endMessage = "Please check this statement carefully and report any errors or unauthorised transactions straight away"

  def buildHeader(header: String): Header = {
    val headerColumns = header.split("[\\s]+")
    val date = headerColumns(0)
    val details = headerColumns(1)
    Header(date, date, details, "amount")
  }

  @tailrec
  private def findTransactionStart(page: Seq[String]): Seq[String] = {
    val droppedLines1 = page.dropWhile(line => line =!= transactionStart)
    val lines = if (droppedLines1.length < 2) {
      val (_, transactionLists) =
        page.span(line => line.split("[\\s]+")
          .map(_.trim.toLowerCase).toList =!= headers)
      transactionLists
    } else {
      droppedLines1.drop(1)
    }
    if (lines.length < 2) {
      Vector.empty[String]
    } else {
      val found = lines.headOption.exists(_.trim.startsWith("Date"))
      if (found) lines else findTransactionStart(lines)
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.Any"))
  def apply(page: Seq[String]): Option[TransactionDoc] = {
    val lines = findTransactionStart(page)
    if (lines.isEmpty) {
      None
    } else {
      val date = P(digits.rep.! ~ "/" ~ digits.rep.! ~ "/" ~ digits.rep.!).map { case (day, month, year) =>
        LocalDate.parse(s"$year-$month-$day")
      }

      val lineP = date ~ spaces.rep ~ AnyChar.rep(1).! ~ End
      val header = lines.headOption.map(buildHeader).getOrElse(Header("", "", "", ""))

      @tailrec
      def collect(lines: Seq[String],  acc: Vector[String]): Vector[String] = lines match {
        case Nil =>
          acc
        case x :: xs =>
          val line = x.trim
          println(
            s"""line: $line
               | [2]: ${xs.take(2)}
               | [5]: ${xs.take(5)}
               |""".stripMargin)
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
                val firstFive = xs.take(5)
                val (beforeNext, next) = firstFive.span(l => isFailure(lineP.parse(l)))
                if (beforeNext.isEmpty)
                  collect(xs, acc :+ line)
                else {
                  println(
                    s"""beforeNext: $beforeNext
                       |      next: $next
                       |""".stripMargin
                  )

                  val validBeforeNext = beforeNext.takeWhile { x =>
                    val y = x.trim
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
                    acc :+ (line +: validBeforeNext.toVector).mkString(" ")
                  } else {
                    collect(next ++ xs.drop(5), acc :+ (line +: validBeforeNext.toVector).mkString(" "))
                  }
                }
              } else {
                collect(xs, acc)
              }
            }
          }

      }

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      def processLine(lines: Vector[String]): Vector[Transaction] = lines.flatMap { line =>
        lineP.parse(line) match {
          case Parsed.Success((d, a), _) =>
            val words = a.split("[\\s]+").map(_.trim)
            val (details, Array(amount, balance)) = words.splitAt(words.length - 2)
            println(
              s"""      a: $a
                 |  words: ${words.mkString}
                 |details: ${details.mkString}
                 | amount: $amount
                 |balance: $balance
                 |""".stripMargin)
            val filteredAmount = amount.replaceAllLiterally(",", "")
            if (filteredAmount.startsWith("-")) {

            }
            Vector(
              Transaction(
                d,
                d,
                details.mkString(" "),
                BigDecimal(
                  if (filteredAmount.startsWith("-"))
                    filteredAmount.drop(1)
                  else
                    s"""-$filteredAmount"""
                )
              )
            )
          case Parsed.Failure(_, _, _) =>
            Vector.empty
        }
      }

      val collected = collect(lines.drop(1), Vector.empty)
      val content = processLine(collected)
      TransactionDoc(header, content).some
    }
  }
}
