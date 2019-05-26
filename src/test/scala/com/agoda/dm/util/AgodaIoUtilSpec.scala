package com.agoda.dm.util

import java.io.{File, IOException, InputStream, SequenceInputStream}
import java.net.URI
import java.nio.charset.StandardCharsets

import com.agoda.dm.TestHelper._
import com.agoda.dm.protocol.Resource
import org.apache.commons.io.{FileUtils, IOUtils}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Try

class AgodaIoUtilSpec extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  val resourceDir = new File("src/test/resources")
  val outFolder = new File(resourceDir.getAbsolutePath + "/out")
  val fileFullPath = outFolder.getAbsolutePath + "/test"

  "AgodaIoUtil" should "save input stream to file" in {
    val testData = "test data"
    val stream = IOUtils.toInputStream(testData, StandardCharsets.UTF_8)
    val testRes = new Resource {
      override def uri: URI = ???

      override def getInputStream: Try[InputStream] = Try(stream)
    }

    AgodaIoUtil.saveTo(testRes, fileFullPath)
    assert(isFileContentCorrect(fileFullPath, testData))
  }

  it should "should not save file if it was partially saved" in {
    val testData = "test data"
    val stream1 = IOUtils.toInputStream(testData, StandardCharsets.UTF_8)
    val exceptionStream = mock(classOf[InputStream])
    when(exceptionStream.read(any(), any(), any())).thenThrow(new IOException("booo!"))

    val stream = new SequenceInputStream(stream1, exceptionStream)

    val testRes = new Resource {
      override def uri: URI = ???

      override def getInputStream: Try[InputStream] = Try(stream)
    }

    AgodaIoUtil.saveTo(testRes, fileFullPath)
    val file = new File(fileFullPath)
    file.exists() shouldBe false
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    cleanUp()
    FileUtils.forceMkdir(outFolder)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    cleanUp()
  }

  after {
    cleanUp()
  }

  private def cleanUp(): Unit = {
    FileUtils.deleteQuietly(outFolder)
  }
}
