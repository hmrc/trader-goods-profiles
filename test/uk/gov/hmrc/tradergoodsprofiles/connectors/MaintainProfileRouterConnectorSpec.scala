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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.responses.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import scala.concurrent.Future

class MaintainProfileRouterConnectorSpec
    extends BaseConnectorSpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val eori = "GB123456789012"
  private val sut  = new MaintainProfileRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder)

    commonSetUp
    when(httpClient.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
  }

  "put" should {
    "return 200 when the profile is successfully updated" in {

      val response = MaintainProfileResponse(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        ukimsNumber = Some("XIUKIM47699357400020231115081800"),
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )
      when(requestBuilder.execute[Either[ServiceError, MaintainProfileResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(sut.put(eori, createRequest(updateProfileRequestData)))

      result.value mustBe response

      withClue("send a request with the right parameters") {
        val expectedUrl =
          UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori")
        verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        verify(requestBuilder).withBody(eqTo(updateProfileRequestData))(any, any, any)
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error" when {
      "router return an error" in {
        val errorResponse = ServiceError(NOT_FOUND, ErrorResponse(correlationId, "any-code", "any-message"))

        when(requestBuilder.execute[Either[ServiceError, MaintainProfileResponse]](any, any))
          .thenReturn(Future.successful(Left(errorResponse)))

        val result = await(sut.put(eori, createRequest(updateProfileRequestData)))

        result.left.value mustBe errorResponse
      }

      "http client throw" in {
        when(requestBuilder.execute[Either[ServiceError, MaintainProfileResponse]](any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.put(eori, createRequest(updateProfileRequestData)))

        result.left.value mustBe ServiceError(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(
            correlationId,
            "INTERNAL_SERVER_ERROR",
            "Could not update profile due to an internal error"
          )
        )
      }
    }
  }

  def updateProfileRequestData: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB987654321098",
             |    "ukimsNumber": "XIUKIM47699357400020231115081800",
             |    "nirmsNumber": "RMS-GB-123456",
             |    "niphlNumber": "6 S12345"
             |
             |}
             |""".stripMargin)

  def createRequest(body: JsValue): Request[JsValue] =
    FakeRequest().withBody(body)
}
