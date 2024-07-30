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

package uk.gov.hmrc.tradergoodsprofiles.connectors

import io.lemonlabs.uri.UrlPath
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.http.Status.{CREATED, NOT_FOUND}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import scala.concurrent.Future

class AdviceRouterConnectorSpec extends BaseConnectorSpec with ScalaFutures with EitherValues with BeforeAndAfterEach {

  private val eori     = "GB123456789012"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  def requestAdviceData: JsValue = Json
    .parse("""
             |{
             |    "requestorName": "Mr.Phil Edwards",
             |    "requestorEmail": "Phil.Edwards@gmail.com"
             |
             |}
             |""".stripMargin)

  def request: Request[JsValue] = FakeRequest().withBody(requestAdviceData)

  private val sut = new AdviceRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder)

    commonSetUp
    when(httpClient.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
  }

  "request advice" should {

    "return 201 when advice is successfully requested" in {

      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.successful(Right(CREATED)))

      val result = await(sut.post(eori, recordId, request))

      result.value mustBe CREATED

      val expectedUrl =
        UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId/advice")
      verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("Accept"                 -> "application/vnd.hmrc.1.0+json")
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(requestAdviceData))(any, any, any)
      verify(requestBuilder).execute(any, any)
    }

    "return an error" when {
      "api return an error" in {
        val errorResponse = ServiceError(NOT_FOUND, ErrorResponse(correlationId, "any-code", "any-message"))
        when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
          .thenReturn(Future.successful(Left(errorResponse)))

        val result = await(sut.post(eori, recordId, request))

        result.left.value mustBe errorResponse
      }

      "api throw" in {
        when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.post(eori, recordId, request))

        result.left.value mustBe ServiceError(
          500,
          ErrorResponse(
            correlationId,
            "INTERNAL_SERVER_ERROR",
            "Could not request advice due to an internal error"
          )
        )
      }
    }

  }
}
