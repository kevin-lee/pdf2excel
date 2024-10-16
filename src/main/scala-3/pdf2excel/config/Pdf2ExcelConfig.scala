package pdf2excel.config

import effectie.core.Fx
import effectie.syntax.all.*
import pdf2excel.Pdf2Excel.FromTo
import pdf2excel.config.Pdf2ExcelConfig.{ExcelConfig, PdfConfig}
import pureconfig.ConfigReader.Result
import pureconfig.generic.derivation.default.*
import pureconfig.{ConfigReader, ConfigSource}

/** @author Kevin Lee
  * @since 2023-06-18
  */
final case class Pdf2ExcelConfig(pdf: PdfConfig, excel: ExcelConfig) derives ConfigReader
object Pdf2ExcelConfig {

  def load[F[*]: Fx]: F[Result[Pdf2ExcelConfig]] =
    pureOrError(ConfigSource.defaultApplication.load[Pdf2ExcelConfig])

  final case class PdfConfig(path: String, fromTo: Option[FromTo]) derives ConfigReader

  final case class ExcelConfig(path: String) derives ConfigReader
}
