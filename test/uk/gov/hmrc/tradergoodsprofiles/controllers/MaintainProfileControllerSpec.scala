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

import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.MockitoSugar.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofiles.models.responses.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants

import scala.concurrent.{ExecutionContext, Future}

class MaintainProfileControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val requestHeaders = Seq(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )

  private val eori          = "GB123456789012"
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService   = mock[UuidService]
  private val routerService = mock[RouterService]

  private val updateProfileRequest = MaintainProfileRequest(
    actorId = "GB987654321098",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345")
  )

  private val updateProfileResponse = MaintainProfileResponse(
    eori,
    "GB987654321098",
    Some("XIUKIM47699357400020231115081800"),
    Some("RMS-GB-123456"),
    Some("6 S12345")
  )

  private val sut = new MaintainProfileController(
    new FakeSuccessAuthAction(),
    new ValidateHeaderAction(uuidService),
    uuidService,
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, routerService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(routerService.updateProfile(mockEq(eori), any[MaintainProfileRequest])(any()))
      .thenReturn(Future.successful(Right(updateProfileResponse)))
  }

  "MaintainProfileController" should {

    "return 200 OK when the profile update is successful" in {
      val request: FakeRequest[JsValue] = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(Json.toJson(updateProfileRequest))

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(updateProfileResponse)
    }

    "return 400 Bad Request when mandatory field actorId is missing from body or is in the wrong format" in {
      val invalidJsonRequest = Json.obj(
        "ukimsNumber" -> "XIUKIM47699357400020231115081800",
        "nirmsNumber" -> "RMS-GB-123456",
        "niphlNumber" -> "6 S12345"
      )

      val result = sut.updateProfile(eori)(FakeRequest().withHeaders(requestHeaders: _*).withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe createMissingFieldExpectedJson(
        ApplicationConstants.InvalidActorMessage,
        ApplicationConstants.InvalidActorId
      )
    }

    "return 400 Bad Request when mandatory field ukimsNumber is missing from body or is in the wrong format" in {
      val invalidJsonRequest = Json.obj(
        "actorId"     -> "GB987654321098",
        "nirmsNumber" -> "RMS-GB-123456",
        "niphlNumber" -> "6 S12345"
      )

      val result = sut.updateProfile(eori)(FakeRequest().withHeaders(requestHeaders: _*).withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe createMissingFieldExpectedJson(
        ApplicationConstants.InvalidOrMissingUkimsNumber,
        ApplicationConstants.InvalidOrMissingUkimsNumberCode
      )
    }

    "return 400 Bad Request when optional field nirmsNumber is in the wrong format" in {
      val invalidJsonRequest = Json.obj(
        "actorId"     -> "GB987654321098",
        "ukimsNumber" -> "XIUKIM47699357400020231115081800",
        "nirmsNumber" -> 123456,
        "niphlNumber" -> "6 S12345"
      )

      val result = sut.updateProfile(eori)(FakeRequest().withHeaders(requestHeaders: _*).withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe createMissingFieldExpectedJson(
        ApplicationConstants.InvalidOrMissingNirmsNumber,
        ApplicationConstants.InvalidOrMissingNirmsNumberCode
      )
    }

    "return 400 Bad Request when optional field niphlNumber is in the wrong format" in {
      val invalidJsonRequest = Json.obj(
        "actorId"     -> "GB987654321098",
        "ukimsNumber" -> "XIUKIM47699357400020231115081800",
        "nirmsNumber" -> "RMS-GB-123456",
        "niphlNumber" -> 123456
      )

      val result = sut.updateProfile(eori)(FakeRequest().withHeaders(requestHeaders: _*).withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe createMissingFieldExpectedJson(
        ApplicationConstants.InvalidOrMissingNiphlNumber,
        ApplicationConstants.InvalidOrMissingNiphlNumberCode
      )
    }

    "return 500 Internal Server Error when the service layer fails" in {
      withAuthorizedTrader()

      val expectedJson  = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal Server Error"
      )
      val errorResponse = ErrorResponse.serverErrorResponse(
        uuidService.uuid,
        "Internal Server Error"
      )
      val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

      when(routerService.updateProfile(any, any)(any))
        .thenReturn(Future.successful(Left(serviceError)))

      val request = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(Json.toJson(updateProfileRequest))

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }

  }

  private def createMissingFieldExpectedJson(errorMessage: String, errorNumber: Int): JsObject =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Seq(
        Json.obj(
          "code"        -> "INVALID_REQUEST_PARAMETER",
          "message"     -> errorMessage,
          "errorNumber" -> errorNumber
        )
      )
    )
}
