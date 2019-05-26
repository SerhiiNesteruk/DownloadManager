package com.agoda.dm.config

import com.typesafe.config.Config

import scala.concurrent.duration.{Duration, FiniteDuration}

final case class AppConfig(destinationFolder: String, maxRetry: Int, maxAwait: Duration, httpConf: HttpConfig)
object AppConfig {
  def apply(config: Config): AppConfig =
    AppConfig (
      destinationFolder = config.getString("download-manager.destination-folder"),
      maxRetry = config.getInt("download-manager.max-retry"),
      maxAwait = config.getDuration("download-manager.max-await").asScalaDuration,
      httpConf = HttpConfig(config.getDuration("download-manager.protocol.http.connection-timeout").asScalaDuration,
        config.getDuration("download-manager.protocol.http.read-timeout").asScalaDuration)
    )

  implicit class FinDuration(d: java.time.Duration) {
    def asScalaDuration: FiniteDuration = scala.concurrent.duration.Duration.fromNanos(d.toNanos)
  }

}

final case class HttpConfig(readTimeout: FiniteDuration, connectionTimeout: FiniteDuration)
final case class FtpConfig(username: String, pwd: String)
