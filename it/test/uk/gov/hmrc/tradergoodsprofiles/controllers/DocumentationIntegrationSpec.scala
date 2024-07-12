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

package uk.gov.hmrc.tradergoodsprofiles.controllers

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, inject}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig

class DocumentationIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with MockitoSugar {

  lazy val wsClient: WSClient  = app.injector.instanceOf[WSClient]
  val mockAppConfig: AppConfig = mock[AppConfig]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .overrides(inject.bind[AppConfig].toInstance(mockAppConfig))
      .overrides(inject.bind[HttpClientV2].to(httpClientV2))
      .build()

  "DocumentationController" should {
    "return the definition specification" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/definition").get())

      response.status mustBe OK
      response.body must not be empty
      response.body must include("api")
    }

    "return a dynamically generated OpenAPI Specification (OAS) without withdraw advice endpoint when withdrawAdviceEnabled is false" in {
      when(mockAppConfig.withdrawAdviceEnabled).thenReturn(false)
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/application.yaml").get())

      response.status mustBe OK
      response.body must not be empty
      response.body must startWith("---")
      response.body must not include "Withdraw your request for advice from HMRC"
    }

    "return a dynamically generated OpenAPI Specification (OAS) with withdraw advice endpoint when withdrawAdviceEnabled is true" in {
      when(mockAppConfig.withdrawAdviceEnabled).thenReturn(true)
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/application.yaml").get())

      response.status mustBe OK
      response.body must not be empty
      response.body must startWith("---")
      response.body must include("Withdraw your request for advice from HMRC")
    }

    "return a 404 if not specification found" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/111.0/nonexistent.yaml").get())

      response.status mustBe NOT_FOUND
    }
  }
}
