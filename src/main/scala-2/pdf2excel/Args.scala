package pdf2excel

import cats.*
import extras.render.Render
import extras.strings.syntax.all.*
import extras.render.syntax.*

import eu.timepit.refined.cats.*
import eu.timepit.refined.types.all.*
import io.estatico.newtype.macros.newtype

import java.io.File
import java.util.Locale

/** @author Kevin Lee
  * @since 2024-10-16
  */
@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion", "org.wartremover.warts.ImplicitParameter"))
object Args {
  implicit val eqFile: Eq[File] = Eq.fromUniversalEquals

  implicit val showFile: Show[File] = Show.show(_.getCanonicalPath)

  final case class Pdf(statementType: Pdf.StatementType, path: Pdf.Path, fromTo: Pdf.FromTo)
  object Pdf {

    implicit val eqPdf: Eq[Pdf]     = derived.semiauto.eq
    implicit val showPdf: Show[Pdf] = derived.semiauto.show

    sealed trait StatementType
    object StatementType {
      case object Cba extends StatementType
      case object Cba2 extends StatementType
      case object Ing extends StatementType
      case object Nab extends StatementType

      def cba: StatementType  = StatementType.Cba
      def cba2: StatementType = StatementType.Cba2

      def ing: StatementType = StatementType.Ing

      def nab: StatementType = StatementType.Nab

      def allStatementTypes: List[StatementType] = List(cba, cba2, ing, nab)

      implicit val eqStatementType: Eq[StatementType]     = Eq.fromUniversalEquals
      implicit val showStatementType: Show[StatementType] = Show.fromToString

      @SuppressWarnings(Array("org.wartremover.warts.ToString"))
      implicit val renderStatementType: Render[StatementType] = Render.render(_.toString.toLowerCase(Locale.ENGLISH))

      def from(s: String): Either[String, StatementType] = s.toLowerCase(Locale.ENGLISH) match {
        case "cbc" => Right(StatementType.cba)
        case "cbc2" => Right(StatementType.cba2)
        case "ing" => Right(StatementType.ing)
        case "nab" => Right(StatementType.nab)
        case unsupported =>
          Left(
            s"Unsupported statement type [given type: $unsupported]. The supported ones are ${allStatementTypes.map(_.render).commaAnd}."
          )
      }
    }

    @newtype case class Path(value: File)
    object Path {
      implicit val eqPath: Eq[Path]     = deriving
      implicit val pathShow: Show[Path] = deriving
    }

    final case class FromTo(from: Option[FromTo.From], to: Option[FromTo.To])
    object FromTo {

      implicit val eqFromTo: Eq[FromTo]     = derived.semiauto.eq
      implicit val fromToShow: Show[FromTo] = derived.semiauto.show

      @newtype case class From(value: PosInt)
      object From {
        implicit val fromEq: Eq[From]     = deriving
        implicit val fromShow: Show[From] = deriving
      }

      @newtype case class To(value: PosInt)
      object To {
        implicit val toEq: Eq[To]     = deriving
        implicit val toShow: Show[To] = deriving
      }
    }
  }

  final case class Excel(path: Excel.Path)
  object Excel {
    @newtype case class Path(value: File)
    object Path {
      implicit val eqPath: Eq[Path]     = deriving
      implicit val pathShow: Show[Path] = deriving
    }
  }
}
