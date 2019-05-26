package com.agoda.dm.protocol

import java.net.URI
import java.nio.charset.StandardCharsets

import com.agoda.dm.config.AppConfig
import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import org.scalatest._

class SftpSpec extends FlatSpec with Matchers with TryValues {

  val filePath = "/dir/sample.txt"
  val content = "some content"

  val conf = ConfigFactory.load()
  val testAppConf = AppConfig.apply(conf)
  val sftpConf = testAppConf.defaultSftp

  "SFtp resource" should "return InputStream if link is correct" in {
    withSftpServer { server =>
      val port = server.getPort
      server.putFile(filePath, content, StandardCharsets.UTF_8)

      val sftpRes = SFtp(new URI(s"sftp://localhost:$port$filePath"), sftpConf)

      val downloaded = sftpRes.getInputStream.map { stream =>
        IOUtils.toString(stream, StandardCharsets.UTF_8)
      }
      downloaded.success.value should be (content)
    }

  }

  it should "return failure if link is broken" in {
    val broken = SFtp(new URI("sftp://non-exists-site.com.ua/non-exist-file"), testAppConf.defaultSftp)
    val stream = broken.getInputStream
    stream.failure.exception should have message "java.net.UnknownHostException: non-exists-site.com.ua"
  }

  it should "return failure if credentials is not correct" in {
    withSftpServer { server =>
      val port = server.getPort
      server.putFile(filePath, content, StandardCharsets.UTF_8)
      server.addUser("some user", "pwd")

      val sftpRes = SFtp(new URI(s"sftp://localhost:$port$filePath"), sftpConf)

      val stream = sftpRes.getInputStream
      stream.failure.exception should have message "Auth fail"
    }
  }

  it should "return failure if file does not exist" in {
    withSftpServer { server =>
      val port = server.getPort
      server.putFile(filePath, content, StandardCharsets.UTF_8)

      val sftpRes = SFtp(new URI(s"sftp://localhost:$port/non-exist-file"), sftpConf)

      val stream = sftpRes.getInputStream
      stream.failure.exception should have message "No such file or directory"
    }
  }

}
