package com.agoda.dm.util

import java.net.{URI, URL}

import com.agoda.dm.{DownloadItem, Entry, MalformedUri, UnsupportedProtocol}
import com.agoda.dm.config.AppConfig
import com.agoda.dm.protocol.{Ftp, Http, SFtp}
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

object Parser extends StrictLogging {

  def parseInput(input: List[String], appConfig: AppConfig): List[Entry] = {
    input.map(_.toLowerCase).map(uri => parse(uri.trim, appConfig)).distinct
  }

  private def parse(uri: String, appConfig: AppConfig): Entry = {
    val tryUrl = Try(new URI(uri))
    tryUrl match {
      case Success(url) =>
        url.getScheme match {
          case "http" | "https" => DownloadItem(Http(url, appConfig.httpConf), fullPath(appConfig.destinationFolder, uri))
          case "ftp" => DownloadItem(Ftp(url, appConfig.getFtpConfig(url.getHost)), fullPath(appConfig.destinationFolder, uri))
          case "sftp" => DownloadItem(SFtp(url, appConfig.getSftpConfig(url.getHost)), fullPath(appConfig.destinationFolder, uri))
          case _ => UnsupportedProtocol(uri)
        }
      case Failure(exception) =>
        logger.warn(s"cant parse uri: $uri", exception)
        MalformedUri(uri)
    }

  }

  private def fullPath(destination: String, uri: String): String = {
    val fileName = uri
      .replace("://", ".")
      .replace('/', '.')
      .replace(':', '-')
    s"$destination/$fileName"
  }

}
