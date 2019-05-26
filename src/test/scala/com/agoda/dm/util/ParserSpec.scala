package com.agoda.dm.util

import java.net.{URI, URL}

import com.agoda.dm.config.AppConfig
import com.agoda.dm.protocol.{Ftp, Http, SFtp}
import com.agoda.dm.{DownloadItem, MalformedUri, UnsupportedProtocol}
import com.typesafe.config.ConfigFactory
import org.scalatest._

class ParserSpec extends FlatSpec with Matchers {

  val conf = ConfigFactory.load()
  val testAppConf = AppConfig.apply(conf)

  "Parser" should "correctly parse http uri" in {
    val testUri = "http://test.com/test"
    val testUri2 = "http://test.com/test2"
    val expectedEntry = DownloadItem(Http(new URI(testUri), testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (expectedEntry)
  }

  it should "correctly parse malformed uri" in {
    val testUri = "http://test.com/test"
    val testUri2 = "wrong @ resouce.rui"
    val correctEntry = DownloadItem(Http(new URI(testUri), testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (correctEntry)
    parsed should contain (MalformedUri(testUri2))
  }

  it should "correctly parse unsupported protocols uri" in {
    val testUri = "http://test.com/test"
    val testUri2 = "s3://amazon.com/test"
    val correctEntry = DownloadItem(Http(new URI(testUri), testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (correctEntry)
    parsed should contain (UnsupportedProtocol(testUri2))
  }

  it should "correctly parse single uri" in {
    val testUri = "http://test.com/test"
    val correctEntry = DownloadItem(Http(new URI(testUri), testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val parsed = Parser.parseInput(List(testUri), testAppConf)
    parsed should have size 1
    parsed should contain (correctEntry)
  }

  it should "correctly parse ftp uri" in {
    val testUri = "ftp://my-site.com/test"
    val testUri2 = "ftp://test.com/test2"
    val expectedEntry = DownloadItem(
      Ftp(new URI(testUri), testAppConf.getFtpConfig("my-site.com")), s"${testAppConf.destinationFolder}/ftp.my-site.com.test"
    )
    val expectedEntry2 = DownloadItem(
      Ftp(new URI(testUri2), testAppConf.getFtpConfig("test.com")), s"${testAppConf.destinationFolder}/ftp.test.com.test2"
    )
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (expectedEntry)
    parsed should contain (expectedEntry2)
  }

  it should "correctly parse sftp uri" in {
    val testUri = "sftp://my-site.com/test"
    val testUri2 = "sftp://test.com/test2"
    val expectedEntry = DownloadItem(
      SFtp(new URI(testUri), testAppConf.getSftpConfig("my-site.com")), s"${testAppConf.destinationFolder}/sftp.my-site.com.test"
    )
    val expectedEntry2 = DownloadItem(
      SFtp(new URI(testUri2), testAppConf.getSftpConfig("test.com")), s"${testAppConf.destinationFolder}/sftp.test.com.test2"
    )
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (expectedEntry)
    parsed should contain (expectedEntry2)
  }

}
