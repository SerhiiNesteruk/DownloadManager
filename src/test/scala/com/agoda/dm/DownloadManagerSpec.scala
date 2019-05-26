package com.agoda.dm

import java.io.{File, InputStream}
import java.net.URI
import java.nio.charset.StandardCharsets

import com.agoda.dm.TestHelper._
import com.agoda.dm.config.AppConfig
import com.agoda.dm.protocol.{Http, Resource}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.Fault
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.{FileUtils, IOUtils}
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.util.Try

class DownloadManagerSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  implicit val ec = ExecutionContext.global
  val conf = ConfigFactory.load()
  val resourceDir = new File("src/test/resources")
  val downloadFolder = new File(resourceDir.getAbsolutePath + "/downloads")
  val testAppConf = AppConfig.apply(conf).copy(destinationFolder = downloadFolder.getAbsolutePath)
  val dlManager = new DownloadManager(testAppConf.maxRetry)


  var wireMockServer: WireMockServer = _

  "Download manager" should "download all items" in {

    val fileName1 = "res1"
    val path1 = "/my/resource1"
    val dlItem1 = getHttpDownloadItem(path1, fileName1)
    val fileContent1 = "content1"
    generateStub(path1, fileContent1)

    val fileName2 = "res2"
    val path2 = "/my/resource2"
    val dlItem2 = getHttpDownloadItem(path2, fileName2)
    val fileContent2 = "content2"
    generateStub(path2, fileContent2)

    val res = dlManager.downloadAll(List(dlItem1, dlItem2))

    res.map {case (complete , failed) =>
      assert(complete.size == 2)
      assert(failed.isEmpty)
      assert(complete.contains(SuccessfulDownload(dlItem1)))
      assert(complete.contains(SuccessfulDownload(dlItem2)))
      assert(isFileContentCorrect(dlItem1.destinationPath, fileContent1))
      assert(isFileContentCorrect(dlItem2.destinationPath, fileContent2))
    }
  }

  it should "not fail if some files cannot be downloaded" in {

    val fileName1 = "res1"
    val path1 = "/my/resource1"
    val dlItem1 = getHttpDownloadItem(path1, fileName1)
    val fileContent1 = "content1"
    generateStub(path1, fileContent1)

    val failPath = "/failed"
    val failFileName = "boom!"
    val failItem = getHttpDownloadItem(failPath, failFileName)

    stubFor(get(urlEqualTo(failPath))
      .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)))

    val res = dlManager.downloadAll(List(dlItem1, failItem))

    res.map {case (complete , failed) =>
      assert(complete.size == 1)
      assert(failed.size == 1)
      assert(complete.contains(SuccessfulDownload(dlItem1)))
      assert(isFileContentCorrect(dlItem1.destinationPath, fileContent1))
      assert(downloadFolder.list().length == 1)
    }
  }

  "Download manager" should "try to download file few times before give up" in {
    val fileName = "exceptions"
    val testData = "test data"
    val stream = IOUtils.toInputStream(testData, StandardCharsets.UTF_8)
    val failStream: InputStream = null

    val testRes = new Resource {
      var counter = 0

      override def uri: URI = ???

      override def getInputStream: Try[InputStream] = {
        if (counter == 2) Try(stream)
        else {
          counter = counter + 1
          Try(failStream)
        }
      }
    }

    val dlItem = DownloadItem(testRes, s"${testAppConf.destinationFolder}/$fileName")

    val res = dlManager.downloadAll(List(dlItem))

    res.map {case (complete, failed) =>
      assert(complete.size == 1)
      assert(failed.isEmpty)
      assert(complete.contains(SuccessfulDownload(dlItem)))
      assert(isFileContentCorrect(dlItem.destinationPath, testData))
      assert(downloadFolder.list().length == 1)
    }
  }


  private def generateStub(path: String, content: String) = {
    stubFor(get(urlEqualTo(path))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/octet-stream")
        .withBody(content)))
  }

  private def getHttpDownloadItem(uriPath: String, fileName: String) = {
    val httpRes = Http(new URI(s"http://localhost:8080$uriPath"), testAppConf.httpConf)
    DownloadItem(httpRes, s"${testAppConf.destinationFolder}/$fileName")
  }

  override protected def beforeAll(): Unit = {
    wireMockServer = new WireMockServer(wireMockConfig().port(8080))
    wireMockServer.start()
    cleanUp()
    FileUtils.forceMkdir(downloadFolder)

    super.beforeAll()
  }

  after {
    cleanUp()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  private def cleanUp(): Unit = {
    FileUtils.deleteQuietly(downloadFolder)
  }
}
