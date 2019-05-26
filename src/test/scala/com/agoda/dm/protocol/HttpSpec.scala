package com.agoda.dm.protocol

import java.io.File

import com.agoda.dm.config.AppConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest._

import scala.io.Source

class HttpSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val conf = ConfigFactory.load()
  val testAppConf = AppConfig.apply(conf)
  val resourceDir = new File("src/test/resources")
  val outFolder = new File(resourceDir.getAbsolutePath + "/out")
  val fileFullPath = outFolder.getAbsolutePath + "/test"


  var wireMockServer: WireMockServer = _

  "Http resource" should "save file to local filesystem" in {

    val httpRes = Http("http://localhost:8080/my/resource", testAppConf.httpConf)
    val fileContent = "content"

    stubFor(get(urlEqualTo("/my/resource"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/octet-stream")
        .withBody(fileContent)))

    httpRes.save(fileFullPath)
    val fromFile = Source.fromFile(new File(fileFullPath))
    fromFile.mkString === fileContent
  }

  override protected def beforeAll(): Unit = {
    wireMockServer = new WireMockServer(wireMockConfig().port(8080))
    wireMockServer.start()
    cleanUp()
    FileUtils.forceMkdir(outFolder)

    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    cleanUp()
    super.afterAll()
  }

  private def cleanUp(): Unit = {
    FileUtils.deleteQuietly(outFolder)
  }
}
