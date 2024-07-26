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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.connectors.WithdrawAdviceRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, FakeUserAllowListAction}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class WithdrawAdviceControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request                       = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId                      = UUID.randomUUID().toString
  private val correlationId                 = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService                   = mock[UuidService]
  private val withdrawAdviceRouterConnector = mock[WithdrawAdviceRouterConnector]
  private val sut                           = new WithdrawAdviceController(
    new FakeSuccessAuthAction(),
    new FakeUserAllowListAction(),
    withdrawAdviceRouterConnector,
    uuidService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, withdrawAdviceRouterConnector)
    when(uuidService.uuid).thenReturn(correlationId)
    when(withdrawAdviceRouterConnector.withdrawAdvice(any, any, any)(any))
      .thenReturn(Future.successful(Right(NO_CONTENT)))
  }

  "requestAdvice" should {
    "return 204 when withdraw advice is successfully" in {
      val requestBody = createWithdrawAdviceRequest

      val result = sut.withdrawAdvice(eoriNumber, recordId)(request.withBody(Json.toJson(requestBody)))

      status(result) mustBe NO_CONTENT
    }

    "return 204 when withdrawReason is missing" in {

      val result = sut.withdrawAdvice(eoriNumber, recordId)(request.withBody(Json.obj()))

      status(result) mustBe NO_CONTENT
    }

    "return 500 when the router service returns an error" in {
      val withdrawAdviceRequest = createWithdrawAdviceRequest

      val expectedJson = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Could not request advice due to an internal error"
      )

      val errorResponse =
        ErrorResponse.serverErrorResponse(uuidService.uuid, "Could not request advice due to an internal error")
      val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

      when(withdrawAdviceRouterConnector.withdrawAdvice(any, any, any)(any))
        .thenReturn(Future.successful(Left(serviceError)))

      val result = sut.withdrawAdvice(eoriNumber, recordId)(request.withBody(Json.toJson(withdrawAdviceRequest)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }
  }

  def createWithdrawAdviceRequest: JsValue = Json
    .parse("""
             |{
             |    "withdrawReason": "text"
             |}
             |""".stripMargin)
}
