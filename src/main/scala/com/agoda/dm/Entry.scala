package com.agoda.dm

import com.agoda.dm.protocol.Resource

sealed trait Entry
final case class DownloadItem(resource: Resource, destinationPath: String) extends Entry
final case class UnsupportedProtocol(uri: String) extends Entry
final case class MalformedUri(uri: String) extends Entry


