package com.agoda.dm.config

import com.agoda.dm.config.AppConfig.CommonConf
import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}

final case class AppConfig(raw: Config, destinationFolder: String, maxRetry: Int,
                           maxAwait: Duration, httpConf: HttpConfig, defaultFtp: FtpConfig, defaultSftp: SftpConfig) {
  def getFtpConfig(host: String): FtpConfig = {
    val commonConf = getCommonConf("ftp", host)

    FtpConfig(commonConf.usrname.getOrElse(defaultFtp.username),
      commonConf.pwd.getOrElse(defaultFtp.pwd),
      commonConf.port.getOrElse(defaultFtp.port))
  }

  def getSftpConfig(host: String): SftpConfig = {
    val common = getCommonConf("sftp", host)
    SftpConfig(common.usrname.getOrElse(defaultSftp.username),
      common.pwd.getOrElse(defaultSftp.pwd),
      common.port.getOrElse(defaultSftp.port))
  }

  def getCommonConf(protocol: String, host: String): CommonConf = {
    val basePath = s"download-manager.protocol.$protocol.$host"
    val usr = if (raw.hasPath(s"$basePath.username")) Some(raw.getString(s"$basePath.username")) else
      None

    val pwd = if (raw.hasPath(s"$basePath.pwd")) Some(raw.getString(s"$basePath.pwd")) else
      None

    val port = if (raw.hasPath(s"$basePath.port")) Some(raw.getInt(s"$basePath.port")) else
      None
    CommonConf(usr, pwd, port)
  }
}

object AppConfig {
  def apply(config: Config): AppConfig =
    AppConfig(
      raw = config,
      destinationFolder = config.getString("download-manager.destination-folder"),
      maxRetry = config.getInt("download-manager.max-retry"),
      maxAwait = config.getDuration("download-manager.max-await").asScalaDuration,
      httpConf = HttpConfig(config.getDuration("download-manager.protocol.http.connection-timeout").asScalaDuration,
        config.getDuration("download-manager.protocol.http.read-timeout").asScalaDuration),
      defaultFtp = FtpConfig(
        config.getString("download-manager.protocol.ftp.default.username"),
        config.getString("download-manager.protocol.ftp.default.pwd"),
        config.getInt("download-manager.protocol.ftp.default.port")
      ),
      defaultSftp = SftpConfig(
        config.getString("download-manager.protocol.sftp.default.username"),
        config.getString("download-manager.protocol.sftp.default.pwd"),
        config.getInt("download-manager.protocol.sftp.default.port")
      )
    )


  implicit class FinDuration(d: java.time.Duration) {
    def asScalaDuration: FiniteDuration = scala.concurrent.duration.Duration.fromNanos(d.toNanos)
  }

  case class CommonConf(usrname: Option[String], pwd: Option[String], port: Option[Int])

}

final case class HttpConfig(readTimeout: FiniteDuration, connectionTimeout: FiniteDuration)
final case class FtpConfig(username: String, pwd: String, port: Int)
final case class SftpConfig(username: String, pwd: String, port: Int)
