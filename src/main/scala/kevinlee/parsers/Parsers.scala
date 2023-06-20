package kevinlee.parsers

import cats.parse.{Parser => P, _}
import cats.parse.Parser.{Error => ParserError}

import scala.annotation.nowarn

/** @author Kevin Lee
  * @since 2018-03-04
  */
@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing", "org.wartremover.warts.AnyVal"))
object Parsers {

  type Parsed[A] = Either[ParserError, A]

  def isSuccess[T](parsed: Parsed[T]): Boolean = parsed match {
    case Right(_) => true
    case Left(ParserError(_, _) | _: ParserError) => false
  }

  def isFailure[T](parsed: Parsed[T]): Boolean = !isSuccess(parsed)

  final case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
    def apply(t: T): V            = f(t)
    override val toString: String = name
  }

  type CharToParse = NamedFunction[Char, Boolean]

  val Digit: CharToParse =
    NamedFunction('0' to '9' contains (_: Char), "Digit")
  val digits: P[Unit]    = P.charsWhile(Digit).void

  val expondent: P[Unit] = (P.charIn("eE") ~ P.charIn("+-").? ~ digits).void
  val factional: P[Unit] = (P.charIn(".") ~ digits.rep).void
  @nowarn(value ="msg=a type was inferred to be `AnyVal`; this may indicate a programming error.")
  val integral: P[Unit]  = ((P.char('0') | P.charIn('1' to '9')) ~ digits.?).void

  val numbers: Parser0[BigDecimal] =
    (P.charIn("+-").? ~ integral ~ factional.? ~ expondent.?).string.map(BigDecimal(_))

  /* ,digit
   * e.g.) ,000
   */
  val commaDigits: P[Unit] = (P.charIn(',') ~ integral.rep(min = 1)).void

  val monetaryNumbers: Parser0[BigDecimal] =
    (P.charIn("+-").? ~ integral ~ commaDigits.rep ~ factional.? ~ expondent.?)
      .string
      .map(x => BigDecimal(x.replace(",", "")))

  val Whitespace: CharToParse =
    NamedFunction(" \t".contains(_: Char), "Whitespace")
  val spaces: P[Unit]         = P.charsWhile(Whitespace).void

  val NonWhitespace: CharToParse =
    NamedFunction(c => !Whitespace(c), "NonWhitespace")
  val notSpaces: P[Unit]         = P.charsWhile(NonWhitespace).void

  val NewLines: CharToParse =
    NamedFunction("\r\n" contains (_: Char), "Whitespace")
  val newLines: P[Unit]     = P.charsWhile(NewLines).void

  val AlphabetLower: CharToParse =
    NamedFunction(('a' to 'z').contains(_: Char), "AlphabetLower")

  val AlphabetUpper: CharToParse =
    NamedFunction(('A' to 'Z').contains(_: Char), "AlphabetUpper")

  val StringChar: CharToParse =
    NamedFunction(!"''\\".contains(_: Char), "StringChar")

  val NonWhitespaceStringChar: CharToParse =
    NamedFunction(c => NonWhitespace(c) && !StringChar(c), "NonWhitespaceStringChar")

  val alphabetsLower: P[String] = P.charsWhile(AlphabetLower).string

  val alphabetsUpper: P[String] = P.charsWhile(AlphabetUpper).string

  val alphabets: P[String] = (alphabetsLower | alphabetsUpper).rep(min = 1).string

  val hexDigit: P[Unit] = P.charIn(('0' to '9') ++ ('a' to 'f') ++ ('A' to 'F')).void

  val unicodeEscape: P[Unit] = (P.char('u') ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit).void

  @SuppressWarnings(Array("org.wartremover.warts.JavaSerializable", "org.wartremover.warts.Serializable"))
  @nowarn(value = "msg=a type was inferred to be `AnyVal`; this may indicate a programming error.")
  val escape: P[Any] =
    (P.string("""\""") ~ (P.charIn("""/"\bfnrt""") | unicodeEscape)) | P.string("''").map(_ => "'")

  val stringChars: P[String] = P.charsWhile(StringChar).string
  val strings: P[String] = (P.string("'") ~ (stringChars | escape).rep.map(_.toList.mkString) ~ P.string("'")).string

  val nonWhiteSpaceStringChars: P[String] = P.charsWhile(NonWhitespaceStringChar).string

}
