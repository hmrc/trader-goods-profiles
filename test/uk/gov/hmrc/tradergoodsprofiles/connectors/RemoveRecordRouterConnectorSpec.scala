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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import java.util.UUID
import scala.concurrent.Future

class RemoveRecordRouterConnectorSpec
    extends BaseConnectorSpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val eori     = "GB123456789012"
  private val recordId = UUID.randomUUID().toString
  private val actorId  = "GB987654321098"

  private val sut = new RemoveRecordRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder, uuidService)

    commonSetUp
    when(httpClient.delete(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(appConfig.isDrop2Enabled).thenReturn(false)
    when(appConfig.acceptHeaderDisabled).thenReturn(false)
    when(appConfig.isClientIdOptional).thenReturn(false)
  }

  "remove" should {

    "return 204" in {
      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = await(sut.removeRecord(eori, recordId, actorId))

      result.value mustBe NO_CONTENT
      withClue("send a request with the right url when drop2Enabled is false") {
        val expectedUrl =
          s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$actorId"
        verify(httpClient).delete(eqTo(url"$expectedUrl"))(any)
        //Todo: set header can be removed for drop2 - TGP-2029
        verify(requestBuilder).setHeader("X-Client-ID" -> "clientId")
        verify(requestBuilder).setHeader("Accept"      -> "application/vnd.hmrc.1.0+json")
        verify(requestBuilder).execute(any, any)
      }
    }

    //Todo: keep this for drop2 - TGP-2029
    "return 204 when drop2Enabled is true" in {
      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))
      when(appConfig.isDrop2Enabled).thenReturn(true)
      when(appConfig.acceptHeaderDisabled).thenReturn(true)

      await(sut.removeRecord(eori, recordId, actorId))

      val expectedUrl =
        s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$actorId"
      verify(httpClient).delete(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder, never()).setHeader(any, any)
      verify(requestBuilder).execute(any, any)
    }

    "return 204 when acceptHeaderDisabled is true" in {
      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))
      when(appConfig.isClientIdOptional).thenReturn(true)
      when(appConfig.acceptHeaderDisabled).thenReturn(true)

      await(sut.removeRecord(eori, recordId, actorId))

      val expectedUrl =
        s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$actorId"
      verify(httpClient).delete(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder, never()).setHeader(any, any)
      verify(requestBuilder).execute(any, any)
    }

    "return 204 when isClientIdOptional is true" in {
      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))
      when(appConfig.isClientIdOptional).thenReturn(true)

      await(sut.removeRecord(eori, recordId, actorId))

      val expectedUrl =
        s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$actorId"
      verify(httpClient).delete(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder, never()).setHeader(any, any)
      verify(requestBuilder).execute(any, any)
    }

    "return an error response" when {
      "router API return an error" in {
        val expectedErrorResponse = ErrorResponse("123", "code", "error")
        val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
        when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
          .thenReturn(Future.successful(Left(expectedResponse)))

        val result = await(sut.removeRecord(eori, recordId, actorId))

        result.left.value mustBe expectedResponse
      }
    }

    "throw an exception" in {

      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.removeRecord(eori, recordId, actorId))

      val expectedErrorResponse = ErrorResponse(
        correlationId,
        "INTERNAL_SERVER_ERROR",
        s"Could not remove record for eori number $eori, record ID $recordId, and actor ID $actorId"
      )
      val expectedResponse      = ServiceError(INTERNAL_SERVER_ERROR, expectedErrorResponse)

      result.left.value mustBe expectedResponse
    }
  }

}
