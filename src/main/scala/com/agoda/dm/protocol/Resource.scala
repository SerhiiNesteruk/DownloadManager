package com.agoda.dm.protocol

import java.io.InputStream
import java.net.URI

import com.agoda.dm.config.{FtpConfig, HttpConfig, SftpConfig}
import com.jcraft.jsch.{ChannelSftp, JSch}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.net.ftp.FTPClient

import scala.util.Try

trait Resource {
  def uri: URI
  def getInputStream: Try[InputStream]
}

final case class Http(uri: URI, conf: HttpConfig) extends Resource with StrictLogging {
  override def getInputStream: Try[InputStream] = {
    logger.debug(s"Started downloading $uri")
    Try {
      val connection = uri.toURL.openConnection()
      connection.setConnectTimeout(conf.connectionTimeout.toMillis.toInt)
      connection.setReadTimeout(conf.readTimeout.toMillis.toInt)
      connection.getInputStream
    }
  }
}

final case class Ftp(uri: URI, conf: FtpConfig) extends Resource with StrictLogging {
  val client = new FTPClient

  override def getInputStream: Try[InputStream] = {
    logger.debug(s"Start ftp downloading $uri using $conf")
    Try {
      val port = if (uri.getPort == -1) conf.port else uri.getPort
      client.connect(uri.getHost, port)
      if (!client.login(conf.username, conf.pwd)) throw new Exception("Invalid credentials")
      val stream = client.retrieveFileStream(uri.getPath)
      if (stream == null) throw new Exception(s"Cant get stream, error code: ${client.getReplyCode} ${client.getReplyString}")
      else stream

    }
  }
}

final case class SFtp(uri: URI, conf: SftpConfig) extends Resource with StrictLogging {
  val jsch = new JSch()

  override def getInputStream: Try[InputStream] = {
    logger.debug(s"Start sftp downloading $uri using $conf")
    Try {
      val port = if (uri.getPort == -1) conf.port else uri.getPort
      val session = jsch.getSession(conf.username, uri.getHost, port)
      session.setConfig("StrictHostKeyChecking", "no")
      session.setPassword(conf.pwd)
      session.connect()

      val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
      channel.connect()
      channel.get(uri.getPath)
    }
  }
}

