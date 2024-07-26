/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.tradergoodsprofiles.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WireMockServerSpec {

  val wireHost                               = "localhost"
  implicit lazy val wireMock: WireMockServer = new WireMockServer(options().dynamicPort())

  def configureServices: Map[String, Any] =
    Map(
      "microservice.services.trader-goods-profiles-router.host" -> wireHost,
      "microservice.services.trader-goods-profiles-router.port" -> wireMock.port(),
      "microservice.services.user-allow-list.host"              -> wireHost,
      "microservice.services.user-allow-list.port"              -> wireMock.port()
    )

  def stubForUserAllowList: StubMapping =
    wireMock.stubFor(
      post(urlEqualTo(s"/user-allow-list/trader-goods-profiles/private-beta/check"))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

  def stubForUserAllowListWhereUserItNotAllowed: StubMapping =
    wireMock.stubFor(
      post(urlEqualTo(s"/user-allow-list/trader-goods-profiles/private-beta/check"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )
}
