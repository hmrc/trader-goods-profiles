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
import play.api.libs.json.JsValue
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.UpdateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

class UpdateRecordRouterConnectorSpec
    extends BaseConnectorSpec
    with ScalaFutures
    with EitherValues
    with CreateOrUpdateRecordResponseSupport
    with UpdateRecordRequestSupport
    with BeforeAndAfterEach {

  private val eori                                        = "GB123456789012"
  private val recordId                                    = UUID.randomUUID().toString
  private val createUpdateRecordRequest: Request[JsValue] = FakeRequest().withBody(createUpdateRecordRequestData)

  private val sut = new UpdateRecordRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder, uuidService)

    commonSetUp
    when(httpClient.patch(any)(any)).thenReturn(requestBuilder)
    when(httpClient.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any))
      .thenReturn(requestBuilder)
    when(appConfig.sendClientId).thenReturn(true)
  }

  "patch" should {

    "return 200" in {
      val response = createCreateOrUpdateRecordResponse(recordId, eori, Instant.now)
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(sut.patch(eori, recordId, createUpdateRecordRequest))

      result.value mustBe response

      withClue("send a request with the right url") {
        val expectedUrl =
          UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId")
        verify(httpClient).patch(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        verify(requestBuilder).setHeader("Accept"                 -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
        verify(requestBuilder).withBody(eqTo(createUpdateRecordRequestData))(any, any, any)
        verify(requestBuilder).execute(any, any)
      }
    }

    "should not send client ID is features flag sendClientId is false" in {
      when(appConfig.sendClientId).thenReturn(false)
      val response = createCreateOrUpdateRecordResponse(recordId, eori, Instant.now)
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(sut.patch(eori, recordId, createUpdateRecordRequest))

      result.value mustBe response

      withClue("send a request with the right url") {
        val expectedUrl =
          UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId")
        verify(httpClient).patch(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        verify(requestBuilder).setHeader("Accept"                 -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).withBody(eqTo(createUpdateRecordRequestData))(any, any, any)
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error" when {
      "router connector return an error" in {
        val expectedErrorResponse = ErrorResponse("123", "code", "error")
        val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
        when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
          .thenReturn(Future.successful(Left(expectedResponse)))

        val result = await(sut.patch(eori, recordId, createUpdateRecordRequest))

        result.left.value mustBe expectedResponse
      }

      "http client throw" in {

        when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.patch(eori, recordId, createUpdateRecordRequest))

        result.left.value mustBe expectedResponse
      }
    }
  }

  "put" should {

    "return 200" in {
      val response = createCreateOrUpdateRecordResponse(recordId, eori, Instant.now)
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(sut.put(eori, recordId, createUpdateRecordRequest))

      result.value mustBe response

      withClue("send a request with the right url") {
        val expectedUrl =
          UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId")
        verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        verify(requestBuilder).setHeader("Accept"                 -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).withBody(eqTo(createUpdateRecordRequestData))(any, any, any)
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error" when {
      "router connector return an error" in {
        val expectedErrorResponse = ErrorResponse("123", "code", "error")
        val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
        when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
          .thenReturn(Future.successful(Left(expectedResponse)))

        val result = await(sut.put(eori, recordId, createUpdateRecordRequest))

        result.left.value mustBe expectedResponse
      }

      "http client throw" in {
        when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = await(sut.put(eori, recordId, createUpdateRecordRequest))

        result.left.value mustBe expectedResponse
      }
    }
  }

  private def expectedResponse = {
    val expectedErrorResponse = ErrorResponse(
      correlationId,
      "INTERNAL_SERVER_ERROR",
      "Could not update record due to an internal error"
    )
    ServiceError(INTERNAL_SERVER_ERROR, expectedErrorResponse)
  }
}
