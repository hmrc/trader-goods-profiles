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

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.inject.bind
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig

class DocumentationIntegrationSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach {

  private val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val appConfig = mock[AppConfig]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .overrides(
        bind[AppConfig].toInstance(appConfig)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig)
    when(appConfig.putMethodEnabled).thenReturn(false)
  }
  "DocumentationController" should {
    "return the definition specification" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/definition").get())

      response.status mustBe OK
      response.body must not be empty
      response.body must include("api")
    }

    "return an OpenAPi Specification (OAS)" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/application.yaml").get())

      response.status mustBe OK
      response.body must not be empty
    }

    "return an OpenAPi Specification (OAS) without updateRecord (PUT) endpoint" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/application.yaml").get())

      response.status mustBe OK
      response.body must not.include("updateTraderGoodsProfileRecord")
    }

    "return an OpenAPi Specification (OAS) with updateRecord (PUT) endpoint" in {
      when(appConfig.putMethodEnabled).thenReturn(true)
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/1.0/application.yaml").get())

      response.status mustBe OK
      response.body must include("updateTraderGoodsProfileRecord")
    }

    "return a 404 if not specification found" in {
      val response = await(wsClient.url(s"http://localhost:$port/api/conf/111.0/nonexistent.yaml").get())

      response.status mustBe NOT_FOUND
    }
  }
}
