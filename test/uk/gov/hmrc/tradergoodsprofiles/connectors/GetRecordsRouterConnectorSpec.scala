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
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{GetRecordResponse, GetRecordsResponse}
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import java.time.Instant
import scala.concurrent.Future

class GetRecordsRouterConnectorSpec
    extends BaseConnectorSpec
    with GetRecordResponseSupport
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val eori     = "GB123456789012"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val sut = new GetRecordsRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder, uuidService)

    commonSetUp
    when(uuidService.uuid).thenReturn(correlationId)
    when(httpClient.get(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
  }

  "get single record" should {

    "return 200" in {

      val routerResponse = createGetRecordResponse("eori", "recoreId", Instant.now)
      when(requestBuilder.execute[Either[ServiceError, GetRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(routerResponse)))

      val result = await(sut.get(eori, recordId))

      result.value mustBe routerResponse
      withClue("send a request with the right url") {
        val expectedUrl =
          UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId")
        verify(httpClient).get(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader("Accept"      -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).setHeader("X-Client-ID" -> "clientId")
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error" in {
      val expectedErrorResponse = ErrorResponse("123", "code", "error")
      val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
      when(requestBuilder.execute[Either[ServiceError, GetRecordResponse]](any, any))
        .thenReturn(Future.successful(Left(expectedResponse)))

      val result = await(sut.get(eori, recordId))

      result.left.value mustBe expectedResponse
    }

    "catch exception" in {
      val expectedErrorResponse = ErrorResponse(
        correlationId,
        "INTERNAL_SERVER_ERROR",
        s"Could not retrieve record for eori number $eori and record ID $recordId"
      )
      val expectedResponse      = ServiceError(INTERNAL_SERVER_ERROR, expectedErrorResponse)
      when(requestBuilder.execute[Either[ServiceError, GetRecordResponse]](any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.get(eori, recordId))

      result.left.value mustBe expectedResponse

    }
  }

  "gerRecords" should {
    "return 200" in {
      val routerResponse = createGetRecordsResponse("eori", "recoreId", Instant.now)
      when(requestBuilder.execute[Either[ServiceError, GetRecordsResponse]](any, any))
        .thenReturn(Future.successful(Right(routerResponse)))

      val result = await(sut.get(eori))

      result.value mustBe routerResponse

      withClue("send a request with the right url") {
        val expectedUrl =
          UrlPath.parse(s"http://localhost:23123/trader-goods-profiles-router/traders/$eori/records")
        verify(httpClient).get(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader("Accept"      -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).setHeader("X-Client-ID" -> "clientId")
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error" in {
      val expectedErrorResponse = ErrorResponse("123", "code", "error")
      val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
      when(requestBuilder.execute[Either[ServiceError, GetRecordsResponse]](any, any))
        .thenReturn(Future.successful(Left(expectedResponse)))

      val result = await(sut.get(eori))

      result.left.value mustBe expectedResponse
    }

    "catch exception" in {
      val expectedErrorResponse =
        ErrorResponse(correlationId, "INTERNAL_SERVER_ERROR", s"Could not retrieve records for eori number $eori")
      val expectedResponse      = ServiceError(INTERNAL_SERVER_ERROR, expectedErrorResponse)
      when(requestBuilder.execute[Either[ServiceError, GetRecordsResponse]](any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.get(eori))

      result.left.value mustBe expectedResponse

    }

  }
}
