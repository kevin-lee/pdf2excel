package pdf2excel.config

import effectie.core.Fx
import effectie.syntax.all._
import pdf2excel.Pdf2Excel.FromTo
import pdf2excel.config.Pdf2ExcelConfig.{ExcelConfig, PdfConfig}
import pureconfig.ConfigReader.Result
import pureconfig.{ConfigReader, ConfigSource}

/** @author Kevin Lee
  * @since 2023-06-18
  */
final case class Pdf2ExcelConfig(pdf: PdfConfig, excel: ExcelConfig)
object Pdf2ExcelConfig {

  def load[F[*]: Fx]: F[Result[Pdf2ExcelConfig]] =
    pureOrError(ConfigSource.defaultApplication.load[Pdf2ExcelConfig])

  implicit val pdf2ExcelConfigConfigReader: ConfigReader[Pdf2ExcelConfig] = pureconfig.generic.semiauto.deriveReader

  final case class PdfConfig(path: String, fromTo: Option[FromTo])
  object PdfConfig {
    implicit val pdfConfigConfigReader: ConfigReader[PdfConfig] = pureconfig.generic.semiauto.deriveReader
  }

  final case class ExcelConfig(path: String)
  object ExcelConfig {
    implicit val excelConfigConfigReader: ConfigReader[ExcelConfig] = pureconfig.generic.semiauto.deriveReader

  }
}
