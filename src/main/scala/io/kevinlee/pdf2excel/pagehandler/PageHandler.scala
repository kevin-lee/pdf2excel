package io.kevinlee.pdf2excel.pagehandler

/**
  * @author Kevin Lee
  * @since 2018-09-30
  */
trait PageHandler[A] extends (Seq[String] => Option[A])
