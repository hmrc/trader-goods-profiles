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

import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.SwaggerParseResult
import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, _}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig

import scala.concurrent.Future

class DocumentationControllerSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with MockitoSugar {

  val mockAppConfig: AppConfig = mock[AppConfig]

  implicit lazy val materializer: Materializer = app.materializer

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AppConfig].to(mockAppConfig))
    .build()

  override def beforeEach(): Unit =
    reset(mockAppConfig)

  "DocumentationController" when {
    "definition" should {
      "return definition.json file" in {
        val result = doGet("/api/definition", Map.empty)
        status(result) shouldBe OK
        val jsonResult = contentAsJson(result)
        (jsonResult \ "api").asOpt[JsValue] should not be empty
      }
    }

    "specification" should {
      "return valid application.yaml without Request Advice endpoint when requestAdviceEnabled is false" in {
        when(mockAppConfig.requestAdviceEnabled).thenReturn(false)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = contentAsString(result)

        stringResult should include("---")
        stringResult should not include s"/customs/traders/goods-profiles/{eori}/records/{recordId}/advice:"
        stringResult should not include "summary: Ask HMRC for advice if a commodity code is correct"

        validateOpenAPISpec(stringResult)
      }

      "return valid application.yaml with Request Advice endpoint when requestAdviceEnabled is true" in {
        when(mockAppConfig.requestAdviceEnabled).thenReturn(true)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = contentAsString(result)

        stringResult should include("---")
        stringResult should include("/customs/traders/goods-profiles/{eori}/records/{recordId}/advice:")
        stringResult should include("summary: Ask HMRC for advice if a commodity code is correct")

        validateOpenAPISpec(stringResult)
      }

      "return valid application.yaml without withdraw advice endpoint when withdrawAdviceEnabled is false" in {
        when(mockAppConfig.withdrawAdviceEnabled).thenReturn(false)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = contentAsString(result)

        stringResult should include("---")
        stringResult should not include "summary: Withdraw your request for advice from HMRC"

        validateOpenAPISpec(stringResult)
      }

      "return valid application.yaml with withdraw advice endpoint when withdrawAdviceEnabled is true" in {
        when(mockAppConfig.withdrawAdviceEnabled).thenReturn(true)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = contentAsString(result)

        stringResult should include("---")
        stringResult should include("summary: Withdraw your request for advice from HMRC")

        validateOpenAPISpec(stringResult)
      }

      "should return valid application.yaml when both requestAdviceEnabled and withdrawAdviceEnabled are set to false" in {
        when(mockAppConfig.requestAdviceEnabled).thenReturn(false)
        when(mockAppConfig.withdrawAdviceEnabled).thenReturn(false)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = contentAsString(result)

        stringResult should include("---")
        stringResult should not include "/customs/traders/goods-profiles/{eori}/records/{recordId}/advice:"
        stringResult should not include "summary: Ask HMRC for advice if a commodity code is correct"
        stringResult should not include "summary: Withdraw your request for advice from HMRC"

        validateOpenAPISpec(stringResult)
      }

      "should return valid application.yaml when both requestAdviceEnabled and withdrawAdviceEnabled are set to true" in {
        when(mockAppConfig.requestAdviceEnabled).thenReturn(true)
        when(mockAppConfig.withdrawAdviceEnabled).thenReturn(true)
        val result: Future[Result] = doGet("/api/conf/1.0/application.yaml", Map.empty)

        status(result) shouldBe OK
        val stringResult = contentAsString(result)

        stringResult should include("---")
        stringResult should include("/customs/traders/goods-profiles/{eori}/records/{recordId}/advice:")
        stringResult should include("summary: Ask HMRC for advice if a commodity code is correct")
        stringResult should include("summary: Withdraw your request for advice from HMRC")

        validateOpenAPISpec(stringResult)
      }

    }
  }

  def doGet(uri: String, headers: Map[String, String]): Future[Result] = {
    val fakeRequest = FakeRequest(GET, uri).withHeaders(headers.toSeq: _*)
    route(app, fakeRequest).get
  }

  def validateOpenAPISpec(yamlContent: String): Unit = {
    val parser                     = new OpenAPIV3Parser()
    val result: SwaggerParseResult = parser.readContents(yamlContent, null, null)
    if (result.getMessages != null && result.getMessages.size() > 0) {
      fail("Generated YAML is not a valid OpenAPI v3.0 specification: " + result.getMessages)
    } else {
      result.getOpenAPI should not be null
    }
  }
}
