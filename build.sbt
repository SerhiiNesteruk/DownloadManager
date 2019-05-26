name := "AgodaDownloadManager"

version := "0.1"

scalaVersion := "2.12.8"

parallelExecution in Test := false

libraryDependencies += "commons-io" % "commons-io" % "2.6"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe" % "config" % "1.3.4"

libraryDependencies += "commons-net" % "commons-net" % "3.6"

libraryDependencies += "com.github.tomakehurst" % "wiremock-jre8" % "2.23.2" % "test"

libraryDependencies += "org.mockftpserver" % "MockFtpServer" % "2.7.1" % "test"

libraryDependencies += "com.jcraft" % "jsch" % "0.1.55"

libraryDependencies += "org.apache.sshd" % "sshd-sftp" % "2.2.0"

libraryDependencies += "com.github.stefanbirkner" % "fake-sftp-server-lambda" % "1.0.0" % Test

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test

lazy val scalatest =  "org.scalatest" % "scalatest_2.12" % "3.0.5"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies += scalatest % "it,test",
    IntegrationTest / fork := true,
    IntegrationTest / javaOptions += "-Xmx100m" //to test big file download
  )
