package pdf2excel

import cats.*
import cats.derived.*
import refined4s.*
import refined4s.types.all.*
import refined4s.modules.cats.derivation.*
import refined4s.modules.cats.derivation.types.all.given

import java.io.File

/** @author Kevin Lee
  * @since 2024-10-16
  */
object Args {
  given eqFile: Eq[File] = Eq.fromUniversalEquals

  given showFile: Show[File] = Show.show(_.getCanonicalPath)

  final case class Pdf(path: Pdf.Path, fromTo: Pdf.FromTo) derives Eq, Show
  object Pdf {
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
