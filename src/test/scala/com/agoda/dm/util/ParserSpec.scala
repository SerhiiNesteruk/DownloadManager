package com.agoda.dm.util

import com.agoda.dm.config.AppConfig
import com.agoda.dm.protocol.Http
import com.agoda.dm.{DownloadItem, MalformedUri, UnsupportedProtocol}
import com.typesafe.config.ConfigFactory
import org.scalatest._

class ParserSpec extends FlatSpec with Matchers {

  val conf = ConfigFactory.load()
  val testAppConf = AppConfig.apply(conf)

  "Parser" should "correctly parse http uri" in {
    val testUri = "http://test.com/test"
    val testUri2 = "http://test.com/test2"
    val expectedEntry = DownloadItem(Http(testUri, testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (expectedEntry)
  }

  it should "correctly parse malformed uri" in {
    val testUri = "http://test.com/test"
    val testUri2 = "wrong @ resouce.rui"
    val correctEntry = DownloadItem(Http(testUri, testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (correctEntry)
    parsed should contain (MalformedUri(testUri2))
  }

  it should "correctly parse unsupported protocols uri" in {
    val testUri = "http://test.com/test"
    val testUri2 = "s3://amazon.com/test"
    val correctEntry = DownloadItem(Http(testUri, testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val input = List(testUri, testUri2)
    val parsed = Parser.parseInput(input, testAppConf)
    parsed should have size 2
    parsed should contain (correctEntry)
    parsed should contain (UnsupportedProtocol(testUri2))
  }

  it should "correctly parse single uri" in {
    val testUri = "http://test.com/test"
    val correctEntry = DownloadItem(Http(testUri, testAppConf.httpConf), s"${testAppConf.destinationFolder}/http.test.com.test")
    val parsed = Parser.parseInput(List(testUri), testAppConf)
    parsed should have size 1
    parsed should contain (correctEntry)
  }

}
