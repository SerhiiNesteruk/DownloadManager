package com.agoda.dm

import java.io.File

import scala.io.Source

object TestHelper {
  def isFileContentCorrect(path: String, content: String): Boolean = {
    val source = Source.fromFile(new File(path))
    val read = try source.getLines.mkString finally source.close()
    read == content
  }
}
