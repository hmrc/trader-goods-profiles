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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.RouterCreateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import java.time.Instant
import scala.concurrent.Future

class CreateRecordRouterConnectorSpec
    extends BaseConnectorSpec
    with RouterCreateRecordRequestSupport
    with CreateOrUpdateRecordResponseSupport
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val eori = "GB123456789012"

  private val sut = new CreateRecordRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder, uuidService)

    commonSetUp
    when(httpClient.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any))
      .thenReturn(requestBuilder)
    when(appConfig.sendClientId).thenReturn(true)
  }

  "create" should {

    "return 201 when the record is successfully created" in {
      val response = createCreateOrUpdateRecordResponse("any-recordId", eori, Instant.now)
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(sut.createRecord(eori, createRouterCreateRecordRequest))

      result.value mustBe response

      withClue("send a request with the right url") {
        val expectedUrl = UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records")
        verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        verify(requestBuilder).setHeader("Accept"                 -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
        verify(requestBuilder).withBody(eqTo(createRouterCreateRecordRequestData))(any, any, any)
        verify(requestBuilder).execute(any, any)
      }
    }

    "not send the client ID in the header when sendClientId is false" in {
      when(appConfig.sendClientId).thenReturn(false)
      val response = createCreateOrUpdateRecordResponse("any-recordId", eori, Instant.now)
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Right(response)))

      val result = await(sut.createRecord(eori, createRouterCreateRecordRequest))

      result.value mustBe response

      withClue("send a request with the right url") {
        val expectedUrl = UrlPath.parse(s"$serverUrl/trader-goods-profiles-router/traders/$eori/records")
        verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        verify(requestBuilder).setHeader("Accept"                 -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).withBody(eqTo(createRouterCreateRecordRequestData))(any, any, any)
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error" in {
      val expectedErrorResponse = ErrorResponse("123", "code", "error")
      val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.successful(Left(expectedResponse)))

      val result = await(sut.createRecord(eori, createRouterCreateRecordRequest))

      result.left.value mustBe expectedResponse
    }

    "catch exception" in {
      when(requestBuilder.execute[Either[ServiceError, CreateOrUpdateRecordResponse]](any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.createRecord(eori, createRouterCreateRecordRequest))

      val expectedErrorResponse =
        ErrorResponse(correlationId, "INTERNAL_SERVER_ERROR", s"Could not create record for eori number $eori")
      val expectedResponse      = ServiceError(INTERNAL_SERVER_ERROR, expectedErrorResponse)
      result.left.value mustBe expectedResponse

    }
  }

}
