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

class WithdrawAdviceRouterConnectorSpec
    extends BaseConnectorSpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val eori     = "GB123456789012"
  private val recordId = UUID.randomUUID().toString

  private val sut = new WithdrawAdviceRouterConnector(httpClient, appConfig, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder, uuidService)

    commonSetUp
    when(httpClient.delete(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
  }

  "withdraw advice" should {

    "return 204" in {
      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.successful(Right(NO_CONTENT)))
      val withdrawReason = "today"
      val result         = await(sut.withdrawAdvice(eori, recordId, Some(withdrawReason)))

      result.value mustBe NO_CONTENT
      withClue("send a request with the right url") {
        val expectedUrl =
          s"$serverUrl/trader-goods-profiles-router/traders/$eori/records/$recordId/advice?withdrawReason=$withdrawReason"
        verify(httpClient).delete(eqTo(url"$expectedUrl"))(any)
        verify(requestBuilder).setHeader("X-Client-ID" -> "clientId")
        verify(requestBuilder).execute(any, any)
      }
    }

    "return an error response" when {
      "router API return an error" in {
        val expectedErrorResponse = ErrorResponse("123", "code", "error")
        val expectedResponse      = ServiceError(NOT_FOUND, expectedErrorResponse)
        when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
          .thenReturn(Future.successful(Left(expectedResponse)))

        val result = await(sut.withdrawAdvice(eori, recordId, None))

        result.left.value mustBe expectedResponse
      }
    }

    "throw an exception" in {

      when(requestBuilder.execute[Either[ServiceError, Int]](any, any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.withdrawAdvice(eori, recordId, None))

      val expectedErrorResponse = ErrorResponse(
        correlationId,
        "INTERNAL_SERVER_ERROR",
        s"Could not withdraw Advice for eori number $eori, record ID $recordId"
      )
      val expectedResponse      = ServiceError(INTERNAL_SERVER_ERROR, expectedErrorResponse)

      result.left.value mustBe expectedResponse
    }
  }

}
