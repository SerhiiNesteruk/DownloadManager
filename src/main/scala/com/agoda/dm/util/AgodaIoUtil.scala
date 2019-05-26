package com.agoda.dm.util

import java.io.File

import com.agoda.dm.protocol.Resource
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Try}
import scala.util.control.NonFatal

object AgodaIoUtil {
  def saveTo(resource: Resource, destPath: String): Try[Unit] = {
    val trySource = resource.getInputStream
    trySource.map { source =>
      val file = new File(destPath)
      FileUtils.copyInputStreamToFile(source, file)
    }.recoverWith{
      case NonFatal(e) =>
        cleanUpFailed(destPath)
        Failure(e)
    }
  }

  private def cleanUpFailed(path: String): Unit = {
    FileUtils.deleteQuietly(new File(path))
  }
}
