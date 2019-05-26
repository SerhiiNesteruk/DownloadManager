package com.agoda.dm.protocol

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets

import com.agoda.dm.config.AppConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import org.scalatest._

class HttpSpec extends FlatSpec with Matchers with BeforeAndAfterAll with TryValues {

  val conf = ConfigFactory.load()
  val testAppConf = AppConfig.apply(conf)
  val resourceDir = new File("src/test/resources")
  val outFolder = new File(resourceDir.getAbsolutePath + "/out")
  val fileFullPath = outFolder.getAbsolutePath + "/test"


  var wireMockServer: WireMockServer = _

  "Http resource" should "return InputStream if link is correct" in {

    val httpRes = Http(new URI("http://localhost:8080/my/resource"), testAppConf.httpConf)
    val fileContent = "content"

    stubFor(get(urlEqualTo("/my/resource"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/octet-stream")
        .withBody(fileContent)))

    val downloaded = httpRes.getInputStream.map { stream =>
      IOUtils.toString(stream, StandardCharsets.UTF_8)
    }
    downloaded.success.value should be (fileContent)

  }

  it should "return failure if link is broken" in {
    val broken = Http(new URI("http://non-exists-site.com.ua/non-exist-file"), testAppConf.httpConf)
    val stream = broken.getInputStream
    stream.failure.exception should have message "non-exists-site.com.ua"
  }

  override protected def beforeAll(): Unit = {
    wireMockServer = new WireMockServer(wireMockConfig().port(8080))
    wireMockServer.start()

    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

}
