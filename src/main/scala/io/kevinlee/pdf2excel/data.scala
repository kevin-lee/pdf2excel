package io.kevinlee.pdf2excel

import com.github.nscala_time.time.Imports.LocalDate

/**
  * @author Kevin Lee
  * @since 2018-09-30
  */
trait PageHandler[A] extends (Seq[String] => Option[A])

final case class Header(dateProcessed: String,
                        dateOfTransaction: String,
                        details: String,
                        amount: String) {
  def toSeq: Seq[String] = Vector(dateOfTransaction, details, amount)
}

final case class Transaction(dateProcessed: LocalDate,
                             dateOfTransaction: LocalDate,
                             details: String,
                             amount: BigDecimal)

final case class TransactionDoc(header: Header, content: Seq[Transaction]) {
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  lazy override val toString: String =
    s"""TransactionDoc(
       |  $header,
       |  ${"-" * header.toString.length}
       |  ${content.mkString("", "\n  ", "\n")})
       |""".stripMargin
}
