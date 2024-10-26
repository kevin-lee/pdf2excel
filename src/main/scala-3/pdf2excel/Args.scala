package pdf2excel

import cats.*
import cats.derived.*
import extras.render.Render
import extras.render.syntax.*
import extras.strings.syntax.all.*
import refined4s.*
import refined4s.modules.cats.derivation.*
import refined4s.modules.cats.derivation.types.all.given
import refined4s.types.all.*

import java.io.File
import java.util.Locale

/** @author Kevin Lee
  * @since 2024-10-16
  */
object Args {
  given eqFile: Eq[File] = Eq.fromUniversalEquals

  given showFile: Show[File] = Show.show(_.getCanonicalPath)

  final case class Pdf(statementType: Pdf.StatementType, path: Pdf.Path, fromTo: Pdf.FromTo) derives Eq, Show
  object Pdf {

    enum StatementType derives Eq, Show {
      case Cba
      case Cba2
      case Ing
      case Nab
      case Latitude28DegreeGlobal
    }
    object StatementType {

      def cba: StatementType  = StatementType.Cba
      def cba2: StatementType = StatementType.Cba2

      def ing: StatementType = StatementType.Ing

      def nab: StatementType = StatementType.Nab

      def latitude28DegreeGlobal: StatementType = StatementType.Latitude28DegreeGlobal

      def allStatementTypes: List[StatementType] = List(cba, cba2, ing, nab, latitude28DegreeGlobal)

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      given renderStatementType: Render[StatementType] = Render.render(_.toString.toLowerCase(Locale.ENGLISH))

      def from(s: String): Either[String, StatementType] = s.toLowerCase(Locale.ENGLISH) match {
        case "cbc" => Right(StatementType.cba)
        case "cbc2" => Right(StatementType.cba2)
        case "ing" => Right(StatementType.ing)
        case "nab" => Right(StatementType.nab)
        case "latitude28" => Right(StatementType.latitude28DegreeGlobal)
        case unsupported =>
          Left(
            s"Unsupported statement type [given type: $unsupported]. The supported ones are ${allStatementTypes.map(_.render).commaAnd}."
          )
      }
    }

    type Path = Path.Type
    object Path extends Newtype[File], CatsEqShow[File]

    final case class FromTo(from: Option[FromTo.From], to: Option[FromTo.To]) derives Eq, Show
    object FromTo {
      type From = From.Type
      object From extends Newtype[PosInt], CatsEqShow[PosInt]

      type To = To.Type
      object To extends Newtype[PosInt], CatsEqShow[PosInt]
    }
  }

  final case class Excel(path: Excel.Path) derives Eq, Show
  object Excel {
    type Path = Path.Type
    object Path extends Newtype[File], CatsEqShow[File]
  }
}
