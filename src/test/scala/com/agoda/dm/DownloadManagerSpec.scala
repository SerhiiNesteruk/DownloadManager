package com.agoda.dm

import java.io.File

import com.agoda.dm.config.AppConfig
import com.agoda.dm.protocol.Http
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.Fault
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalactic.source.Position
import org.scalatest._

import scala.concurrent.ExecutionContext
import scala.io.Source

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

  it should "try to download file few times before give up" in {
    // TODO implement using wiremock custom extension
    assert(true)
  }

  private def generateStub(path: String, content: String) = {
    stubFor(get(urlEqualTo(path))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/octet-stream")
        .withBody(content)))
  }

  private def getHttpDownloadItem(uriPath: String, fileName: String) = {
    val httpRes = Http(s"http://localhost:8080$uriPath", testAppConf.httpConf)
    DownloadItem(httpRes, s"${testAppConf.destinationFolder}/$fileName")
  }

  private def isFileContentCorrect(path: String, content: String): Boolean = {
    val source = Source.fromFile(new File(path))
    source.mkString == content
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
