package com.agoda.dm.protocol

import java.io.File
import java.net.URL

import com.agoda.dm.config.HttpConfig
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.FileUtils

sealed trait Resource {
  def uri: String
  def save(destinationPath: String): Unit
}

final case class Http(uri: String, conf: HttpConfig) extends Resource with StrictLogging {
  override def save(destinationPath: String): Unit = {
    logger.debug(s"Started downloading $uri")
    FileUtils.copyURLToFile(
      new URL(uri),
      new File(destinationPath),
      conf.connectionTimeout.toMillis.toInt,
      conf.readTimeout.toMillis.toInt)
    logger.debug(s"Finished download $uri")
  }
}

final case class Ftp(uri: String) extends Resource {
  override def save(destinationPath: String): Unit = {
    println("dl from ftp")
  }
}
