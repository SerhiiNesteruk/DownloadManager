package com.agoda.dm.util

import com.agoda.dm.{DownloadItem, Entry, MalformedUri, UnsupportedProtocol}
import com.agoda.dm.config.AppConfig
import com.agoda.dm.protocol.{Ftp, Http}

object Parser {

  def parseInput(input: List[String], appConfig: AppConfig): List[Entry] = {
    input.map(_.toLowerCase).map(uri => parse(uri.trim, appConfig)).distinct
  }

  private def parse(uri: String, appConfig: AppConfig): Entry = {
    if (!uri.contains(':')) MalformedUri(uri)
    else {
      val protocolStr = uri.takeWhile(_ != ':')
      protocolStr match {
        case "http" | "https" => DownloadItem(Http(uri, appConfig.httpConf), fullPath(appConfig.destinationFolder, uri))
        case "ftp" => DownloadItem(Ftp(uri), fullPath(appConfig.destinationFolder, uri))
        case _ => UnsupportedProtocol(uri)
      }
    }
  }

  private def fullPath(destination: String, uri: String): String = {
    val fileName = uri.replace("://", ".").replace('/', '.').replace(':', '-')
    s"$destination/$fileName"
  }
}
