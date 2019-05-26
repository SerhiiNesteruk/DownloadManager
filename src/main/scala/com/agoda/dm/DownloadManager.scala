package com.agoda.dm

import com.agoda.dm.util.AgodaIoUtil
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DownloadManager(maxRetry: Int) extends StrictLogging {

  def downloadAll(dlItems: List[DownloadItem])(implicit ec: ExecutionContext): Future[(List[SuccessfulDownload], List[FailedDownload])] = {
    val allDownloads = Future.sequence(dlItems.map(x => download(x, maxRetry)))
    val failed = allDownloads.map(_.collect { case x: FailedDownload => x })
    val succeed = allDownloads.map(_.collect { case x: SuccessfulDownload => x })
    for {
      f <- failed
      s <- succeed
    } yield (s, f)
  }

  private def download(dlItem: DownloadItem, retries: Int)(implicit ec: ExecutionContext): Future[DownloadResult] = {
    retry(AgodaIoUtil.saveTo(dlItem.resource, dlItem.destinationPath), maxRetry)
      .transform {
        case Success(_) =>
          Success(SuccessfulDownload(dlItem))
        case Failure(ex) =>
          logger.warn(s"failed to download ${dlItem.resource.uri}", ex)
          Success(FailedDownload(dlItem, ex))
      }
  }

  private def retry[T](op: => Try[T], retries: Int)(implicit ec: ExecutionContext): Future[T] = {
    val toFuture = Future(op).flatMap(Future.fromTry)
    toFuture.recoverWith { case _ if retries > 0 => retry(op, retries - 1) }
  }

}

sealed trait DownloadResult

final case class SuccessfulDownload(dlItem: DownloadItem) extends DownloadResult

final case class FailedDownload(dlItem: DownloadItem, origin: Throwable) extends DownloadResult