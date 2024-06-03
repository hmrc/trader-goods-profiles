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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.UpdateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with CreateOrUpdateRecordResponseSupport
    with UpdateRecordRequestSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request       = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId      = UUID.randomUUID().toString
  private val timestamp     = Instant.parse("2024-01-12T12:12:12Z")
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService   = mock[UuidService]
  private val routerService = mock[RouterService]
  private val sut           = new UpdateRecordController(
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
    when(routerService.updateRecord(any, any, any)(any))
      .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))
  }

  "updateRecord" should {
    "return 200 when the record is successfully updated" in {
      val updateRequest = createUpdateRecordRequest()

      val result = sut.updateRecord(eoriNumber, recordId)(request.withBody(Json.toJson(updateRequest)))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))
    }

    "return 400 when actorId is missing" in {
      val invalidJsonRequest = Json.obj(
        "traderRef"                -> "SKU123456",
        "comcode"                  -> "123456",
        "goodsDescription"         -> "Bananas",
        "countryOfOrigin"          -> "GB",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.updateRecord(eoriNumber, recordId)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe createMissingFieldExpectedJson(
        "actorId",
        ApplicationConstants.InvalidActorMessage,
        ApplicationConstants.InvalidActorId
      )
    }

    "return 400 when JSON body doesn’t match the schema" in {
      val invalidJsonRequest = Json.obj(
        "traderRef"                -> "SKU123456",
        "comcode"                  -> "123456",
        "goodsDescription"         -> "Bananas",
        "countryOfOrigin"          -> "GB",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.updateRecord(eoriNumber, recordId)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST

      contentAsJson(result) mustBe createInvalidJsonExpectedJson("actorId", 8)
    }

    "return 500 when the router service returns an error" in {
      val updateRequest = createUpdateRecordRequest()

      val expectedJson  = Json.obj(
        "correlationId" -> uuidService.uuid,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Sorry, the service is unavailable. You'll be able to use the service later"
      )
      val errorResponse =
        ErrorResponse.serverErrorResponse(
          uuidService.uuid,
          "Sorry, the service is unavailable. You'll be able to use the service later"
        )
      val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

      when(routerService.updateRecord(any, any, any)(any))
        .thenReturn(Future.successful(Left(serviceError)))

      val result = sut.updateRecord(eoriNumber, recordId)(request.withBody(Json.toJson(updateRequest)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }
  }

  private def createMissingFieldExpectedJson(fieldName: String, errorMessage: String, errorNumber: Int): JsObject =
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

  private def createInvalidJsonExpectedJson(field: String, errorNumber: Int): JsObject =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Seq(
        Json.obj(
          "code"        -> "INVALID_REQUEST_PARAMETER",
          "message"     -> s"Mandatory field $field was missing from body or is in the wrong format",
          "errorNumber" -> errorNumber
        )
      )
    )
}
