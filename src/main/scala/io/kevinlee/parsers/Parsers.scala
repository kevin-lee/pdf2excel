package io.kevinlee.parsers

import fastparse.all._

/**
  * @author Kevin Lee
  * @since 2018-03-04
  */
object Parsers {

  case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
    def apply(t: T) = f(t)
    override val toString: String = name
  }

  val Digit: NamedFunction[Char, Boolean] =
    NamedFunction('0' to '9' contains (_: Char), "Digit")
  val digits: P[Unit] = P(CharsWhile(Digit))

  val expondent: P[Unit] = P(CharIn("eE") ~ CharIn("+-").? ~ digits)
  val factional: P[Unit] = P(CharIn(".") ~ digits.rep)
  val integral: P[Unit] = P(("0" | CharIn('1' to '9')) ~ digits.?)

  val numbers: P[BigDecimal] =
    P(CharIn("+-").? ~ integral ~ factional.? ~ expondent.?).!.map(BigDecimal(_))


  val Whitespace: NamedFunction[Char, Boolean] =
    NamedFunction(" \t" contains (_: Char), "Whitespace")
  val spaces: P[Unit] = P(CharsWhile(Whitespace))

  val NewLines: NamedFunction[Char, Boolean] =
    NamedFunction("\r\n" contains (_: Char), "Whitespace")
  val newLines: P[Unit] = P(CharsWhile(NewLines))

  val AlphabetLower: NamedFunction[Char, Boolean] =
    NamedFunction('a' to 'z' contains (_: Char), "AlphabetLower")

  val AlphabetUpper: NamedFunction[Char, Boolean] =
    NamedFunction('A' to 'Z' contains (_: Char), "AlphabetUpper")

  val StringChar: NamedFunction[Char, Boolean] =
    NamedFunction(!"''\\".contains(_: Char), "StringChar")

  val alphabetsLower: P[String] = P(CharsWhile(AlphabetLower).!)

  val alphabetsUpper: P[String] = P(CharsWhile(AlphabetUpper).!)

  val alphabets: P[String] = P(alphabetsLower | alphabetsUpper).rep(1).!


  val hexDigit: P[Unit] = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))

  val unicodeEscape: P[Unit] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)


  val escape: P[Any] =
    P("""\""" ~ (CharIn("""/"\bfnrt""") | unicodeEscape)) | P("''").map(_ => "'")

  val stringChars: P[String] = P(CharsWhile(StringChar)).!
  val strings: P[String] = P("'" ~/ (stringChars | escape).rep.map(_.mkString) ~ "'")

}
