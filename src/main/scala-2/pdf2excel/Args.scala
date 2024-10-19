package pdf2excel

import cats.*
import eu.timepit.refined.cats.*
import eu.timepit.refined.types.all.*
import io.estatico.newtype.macros.newtype

import java.io.File

/** @author Kevin Lee
  * @since 2024-10-16
  */
@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion", "org.wartremover.warts.ImplicitParameter"))
object Args {
  implicit val eqFile: Eq[File] = Eq.fromUniversalEquals

  implicit val showFile: Show[File] = Show.show(_.getCanonicalPath)

  final case class Pdf(path: Pdf.Path, fromTo: Pdf.FromTo)
  object Pdf {

    implicit val eqPdf: Eq[Pdf] = derived.semiauto.eq

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
