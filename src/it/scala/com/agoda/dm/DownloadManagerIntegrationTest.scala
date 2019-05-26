package com.agoda.dm

import java.io.File

import com.agoda.dm.config.AppConfig
import com.agoda.dm.util.Parser
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest.{AsyncFlatSpec, BeforeAndAfter, BeforeAndAfterAll, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class DownloadManagerIntegrationTest extends AsyncFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {
  implicit val ec = ExecutionContext.global
  val conf = ConfigFactory.load()
  val resourceDir = new File("src/it/resources")
  val downloadFolder = new File(resourceDir.getAbsolutePath + "/downloads")
  val testAppConf = AppConfig.apply(conf).copy(destinationFolder = downloadFolder.getAbsolutePath)

  val dlManager = new DownloadManager(testAppConf.maxRetry)

  "Download manager" should "download all links from input" in {
    val input = List("https://piccolo.link/sbt-1.2.8.zip",
      "http://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/2.23.2/wiremock-standalone-2.23.2.jar")
    val res = downloadFiles(input)
    res.map { case (correct, failed) =>
      val filePaths = correct.map(_.dlItem.destinationPath)
      filePaths.foreach { path =>
        val file = new File(path)
        assert(file.exists())
        assert(fileSizeMoreThan(file, oneMb))
      }
      assert(correct.length == 2)
      assert(failed.isEmpty)
    }
  }

  "Download manager" should "download and store files only from valid links" in {
    val input = List("http://central.maven.org/maven2/commons-io/commons-io/2.6/commons-io-2.6.jar",
      "s3://example.com",
      "http://repo1.maven.org/non/exists/link",
      "mal:formed:uri")
    val res = downloadFiles(input)
    res.map { case (correct, failed) =>
      val filePaths = correct.map(_.dlItem.destinationPath)
      filePaths.foreach { path =>
        val file = new File(path)
        assert(file.exists())
      }
      assert(correct.length == 1)
      assert(failed.length == 1)
    }
  }

  "Download manager" should "be able to download big files" in {
    val input = List("https://go.skype.com/skypeforlinux-64.rpm")
    val res = downloadFiles(input)
    res.map { case (correct, failed) =>
      val filePaths = correct.map(_.dlItem.destinationPath)
      filePaths.foreach { path =>
        val file = new File(path)
        assert(file.exists())
        assert(fileSizeMoreThan(file, oneMb * 95))
      }
      assert(correct.length == 1)
      assert(failed.isEmpty)
    }
  }

  "Download manager" should "be able to download from ftp" in {
    val input = List("ftp://test.rebex.net/pub/example/readme.txt")
    val res = downloadFiles(input)
    res.map { case (correct, failed) =>
      val filePaths = correct.map(_.dlItem.destinationPath)
      filePaths.foreach { path =>
        val file = new File(path)
        assert(file.exists())
        val source = scala.io.Source.fromFile(file)
        val downloaded = try source.getLines.mkString finally source.close()
        assert(downloaded == ftpFileContent)
      }
      assert(correct.length == 1)
      assert(failed.isEmpty)
    }
  }

  "Download manager" should "be able to download from sftp" in {
    val input = List("sftp://test.rebex.net/pub/example/readme.txt")
    val res = downloadFiles(input)
    res.map { case (correct, failed) =>
      val filePaths = correct.map(_.dlItem.destinationPath)
      filePaths.foreach { path =>
        val file = new File(path)
        assert(file.exists())
        val source = scala.io.Source.fromFile(file)
        val downloaded = try source.getLines.mkString finally source.close()
        assert(downloaded == ftpFileContent)
      }
      assert(correct.length == 1)
      assert(failed.isEmpty)
    }
  }

  private def downloadFiles(input: List[String]): Future[(List[SuccessfulDownload], List[FailedDownload])] = {
    val downloadItems = Parser.parseInput(input, testAppConf).collect { case x: DownloadItem => x }
    dlManager.downloadAll(downloadItems)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    cleanUp()
    FileUtils.forceMkdir(downloadFolder)
  }

  after {
    cleanUp()
  }

  private def cleanUp(): Unit = {
    FileUtils.deleteQuietly(downloadFolder)
  }

  private def fileSizeMoreThan(f: File, size: Long): Boolean = {
    f.length() > size
  }

  val oneMb = 1000000
  val oneKb = 1000

  val ftpFileContent = "Welcome,you are connected using an FTP account used for testing purposes by Rebex FTP for .NET and Rebex FTP/SSL for .NET sample code.Only read access is allowed and download speed is limited to 16KBps.For infomation about Rebex FTP, Rebex FTP/SSL and other Rebex .NET components, please visit our website at http://www.rebex.net/For feedback and support, contact support@rebex.czThanks!"

}
