package uk.gov.hmrc.tradergoodsprofiles.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

trait WireMockServerSpec {

  val wireHost = "localhost"
  implicit lazy val wireMock: WireMockServer = new WireMockServer(options().dynamicPort())

  def configureServices: Map[String, Any] = {
    Map(
      "microservice.services.trader-goods-profiles-router.host" -> wireHost,
      "microservice.services.trader-goods-profiles-router.port" -> wireMock.port(),
    )
  }

}
