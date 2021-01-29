package org.kibanaLoadTest.helpers

object HttpProtocol {

  def getDefaultHeaders(
      buildVersion: String,
      baseUrl: String
  ): Map[String, String] = {
    Map(
      "Connection" -> "keep-alive",
      "kbn-version" -> buildVersion,
      "Content-Type" -> "application/json",
      "Accept" -> "*/*",
      "Origin" -> baseUrl,
      "Sec-Fetch-Site" -> "same-origin",
      "Sec-Fetch-Mode" -> "cors",
      "Sec-Fetch-Dest" -> "empty"
    )
  }

  def getTextHeaders: Map[String, String] =
    Map("Content-Type" -> "text/html; charset=utf-8")
}
