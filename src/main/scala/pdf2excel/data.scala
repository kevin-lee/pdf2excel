package pdf2excel

import com.github.nscala_time.time.Imports.LocalDate

/** @author Kevin Lee
  * @since 2018-09-30
  */
trait PageHandler[A] extends (List[String] => Option[A]) {
  def postProcess(a: A): A
}

final case class Header(
  dateProcessed: String,
  dateOfTransaction: String,
  details: String,
  amount: String
)

object Header {

  def toSeq(header: Header): Seq[String] = header match {
    case Header(
          _,
          dateOfTransaction,
          details,
          amount,
        ) =>
      Vector(dateOfTransaction, details, amount)
  }
}

final case class Transaction(
  dateProcessed: LocalDate,
  dateOfTransaction: LocalDate,
  details: String,
  amount: BigDecimal
)

final case class TransactionDoc(header: Header, content: Seq[Transaction]) {
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  lazy override val toString: String =
    s"""TransactionDoc(
       |  ${header.toString},
       |  ${"-" * header.toString.length}
       |  ${content.mkString("", "\n  ", "\n")})
       |""".stripMargin
}
