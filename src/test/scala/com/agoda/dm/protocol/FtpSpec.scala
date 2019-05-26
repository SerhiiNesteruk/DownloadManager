package com.agoda.dm.protocol

import java.net.URI
import java.nio.charset.StandardCharsets

import com.agoda.dm.config.{AppConfig, FtpConfig}
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import org.mockftpserver.fake.filesystem.{FileEntry, UnixFakeFileSystem}
import org.mockftpserver.fake.{FakeFtpServer, UserAccount}
import org.scalatest._

class FtpSpec extends FlatSpec with Matchers with BeforeAndAfterAll with TryValues {

  val homeDir = "/"
  val filePath = "/dir/sample.txt"
  val content = "some content"

  val conf = ConfigFactory.load()
  val testAppConf = AppConfig.apply(conf)
  val ftpConf = testAppConf.defaultFtp

  "Ftp resource" should "return InputStream if link is correct" in {

    val ftpRes = Ftp(new URI(s"ftp://localhost:$port$filePath"), ftpConf)

    val downloaded = ftpRes.getInputStream.map { stream =>
      IOUtils.toString(stream, StandardCharsets.UTF_8)
    }
    downloaded.success.value should be (content)

  }

  it should "return failure if link is broken" in {
    val broken = Ftp(new URI("ftp://non-exists-site.com.ua/non-exist-file"), testAppConf.defaultFtp)
    val stream = broken.getInputStream
    stream.failure.exception should have message "non-exists-site.com.ua"
  }

  it should "return failure if credentials is not correct" in {
    val broken = Ftp(new URI(s"ftp://localhost:$port$filePath"), FtpConfig("badusername", "bad_pwd", 21))
    val stream = broken.getInputStream
    stream.failure.exception should have message "Invalid credentials"
  }

  var fakeFtpServer: FakeFtpServer = _
  var port: Int = _
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    fakeFtpServer = new FakeFtpServer()
    fakeFtpServer.setServerControlPort(0)

    val fileSystem = new UnixFakeFileSystem()
    fileSystem.add(new FileEntry(filePath, content))
    fakeFtpServer.setFileSystem(fileSystem)

    val userAccount = new UserAccount(ftpConf.username, ftpConf.pwd, homeDir)
    fakeFtpServer.addUserAccount(userAccount)

    fakeFtpServer.start()
    port = fakeFtpServer.getServerControlPort
  }

  override protected def afterAll(): Unit = {
    fakeFtpServer.stop()
    super.afterAll()
  }

}
