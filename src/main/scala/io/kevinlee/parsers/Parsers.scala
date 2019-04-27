package io.kevinlee.parsers

import fastparse.all._

/**
  * @author Kevin Lee
  * @since 2018-03-04
  */
@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Nothing"))
object Parsers {

  def isSuccess[T](parsed: Parsed[T]): Boolean = parsed match {
    case Parsed.Success(_, _) => true
    case Parsed.Failure(_, _, _) => false
  }

  def isFailure[T](parsed: Parsed[T]): Boolean = !isSuccess(parsed)

  final case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
    def apply(t: T): V = f(t)
    override val toString: String = name
  }

  type CharToParse = NamedFunction[Char, Boolean]

  val Digit: CharToParse =
    NamedFunction('0' to '9' contains (_: Char), "Digit")
  val digits: P[Unit] = P(CharsWhile(Digit))

  val expondent: P[Unit] = P(CharIn("eE") ~ CharIn("+-").? ~ digits)
  val factional: P[Unit] = P(CharIn(".") ~ digits.rep)
  val integral: P[Unit] = P(("0" | CharIn('1' to '9')) ~ digits.?)

  val numbers: P[BigDecimal] =
    P(CharIn("+-").? ~ integral ~ factional.? ~ expondent.?).!.
      map(BigDecimal(_))

  /* digit,digit
   * e.g.) 000,000 or 000
   */
  val monetaryDigits: P[Unit] = P(integral ~ ("," ~ integral.rep(min = 1)).?)

  val monetaryNumbers: P[BigDecimal] =
    P(CharIn("+-").? ~ monetaryDigits.rep(min = 1) ~ factional.? ~ expondent.?).!.
      map(x => BigDecimal(x.replaceAllLiterally(",", "")))


  val Whitespace: CharToParse =
    NamedFunction(" \t" contains (_: Char), "Whitespace")
  val spaces: P[Unit] = P(CharsWhile(Whitespace))

  val NonWhitespace: CharToParse =
    NamedFunction(c => !Whitespace(c), "NonWhitespace")
  val notSpaces: P[Unit] = P(CharsWhile(NonWhitespace))

  val NewLines: CharToParse =
    NamedFunction("\r\n" contains (_: Char), "Whitespace")
  val newLines: P[Unit] = P(CharsWhile(NewLines))

  val AlphabetLower: CharToParse =
    NamedFunction('a' to 'z' contains (_: Char), "AlphabetLower")

  val AlphabetUpper: CharToParse =
    NamedFunction('A' to 'Z' contains (_: Char), "AlphabetUpper")

  val StringChar: CharToParse =
    NamedFunction(!"''\\".contains(_: Char), "StringChar")

  val NonWhitespaceStringChar: CharToParse =
    NamedFunction(c => NonWhitespace(c) && !StringChar(c), "NonWhitespaceStringChar")

  val alphabetsLower: P[String] = P(CharsWhile(AlphabetLower).!)

  val alphabetsUpper: P[String] = P(CharsWhile(AlphabetUpper).!)

  val alphabets: P[String] = P(alphabetsLower | alphabetsUpper).rep(1).!


  val hexDigit: P[Unit] = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))

  val unicodeEscape: P[Unit] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)


  val escape: P[Any] =
    P("""\""" ~ (CharIn("""/"\bfnrt""") | unicodeEscape)) | P("''").map(_ => "'")

  val stringChars: P[String] = P(CharsWhile(StringChar)).!
  val strings: P[String] = P("'" ~/ (stringChars | escape).rep.map(_.mkString) ~ "'")

  val nonWhiteSpaceStringChars: P[String] = P(CharsWhile(NonWhitespaceStringChar)).!

}
