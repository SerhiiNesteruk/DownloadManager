package com.agoda.dm

import com.agoda.dm.config.AppConfig
import com.agoda.dm.util.Parser
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{Await, ExecutionContext}

object Main extends App with StrictLogging {
  implicit val ec: ExecutionContext = ExecutionContext.global
  val config = ConfigFactory.load()
  val appConfig = AppConfig(config)

  val entries = Parser.parseInput(args.toList, appConfig)

  reportIncorrectUri(entries)

  val downloadItems = entries.collect { case x: DownloadItem => x}

  val dlManager = new DownloadManager(appConfig.maxRetry)

  val dlResultF = dlManager.downloadAll(downloadItems)
  val (succeed, failed) = Await.result(dlResultF, appConfig.maxAwait)
  reportResult(succeed, failed)



  private def reportIncorrectUri(uris: List[Entry]): Unit = {
    uris.foreach {
      case MalformedUri(uri) => logger.warn(s"Malformed uri: $uri")
      case UnsupportedProtocol(uri) => logger.warn(s"Unsupported protocol: $uri")
      case _ =>
    }
  }

  private def reportResult(success: List[SuccessfulDownload], failed: List[FailedDownload]): Unit = {
    logger.info(s"Successfully downloaded ${success.length} files:")
    success.foreach(e => logger.info(s"${e.dlItem.resource.uri} to ${e.dlItem.destinationPath}"))
    logger.info(s"Failed to download ${failed.length} files: ")
    failed.foreach(e => logger.warn(s"${e.dlItem.resource.uri} because of ${e.origin.getMessage}" ))
  }
}
