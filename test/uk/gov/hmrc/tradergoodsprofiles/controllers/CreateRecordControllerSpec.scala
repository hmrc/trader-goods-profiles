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

import cats.data.EitherT
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.APICreateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.services.{DateTimeService, RouterService}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class CreateRecordControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with CreateRecordResponseSupport
    with APICreateRecordRequestSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request         = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId        = UUID.randomUUID().toString
  private val timestamp       = Instant.parse("2024-01-12T12:12:12Z")
  private val dateTimeService = mock[DateTimeService]
  private val routerService   = mock[RouterService]
  private val sut             = new CreateRecordController(
    new FakeSuccessAuthAction(),
    new ValidateHeaderAction(dateTimeService),
    dateTimeService,
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(dateTimeService, routerService)
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(routerService.createRecord(any, any)(any))
      .thenReturn(EitherT.fromEither(Right(createCreateRecordResponse(recordId, eoriNumber, timestamp))))
  }

  "createRecord" should {
    "return 201 when the record is successfully created" in {
      val createRequest = createAPICreateRecordRequest()

      val result = sut.createRecord(eoriNumber)(request.withBody(Json.toJson(createRequest)))

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.toJson(createCreateRecordResponse(recordId, eoriNumber, timestamp))
    }

    "return 400 when actorId is missing" in {
      val invalidJsonRequest = Json.obj(
        "traderRef"                -> "SKU123456",
        "comcode"                  -> 123456,
        "goodsDescription"         -> "Bananas",
        "countryOfOrigin"          -> "GB",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST

      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"

      val errorMessage = (contentAsJson(result) \ "message" \ "obj.actorId").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 400 when traderRef is missing" in {
      val invalidJsonRequest = Json.obj(
        "actorId"                  -> "GB987654321098",
        "comcode"                  -> 123456,
        "goodsDescription"         -> "Bananas",
        "countryOfOrigin"          -> "GB",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"
      val errorMessage = (contentAsJson(result) \ "message" \ "obj.traderRef").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 400 when comcode is missing" in {
      val invalidJsonRequest = Json.obj(
        "actorId"                  -> "GB987654321098",
        "traderRef"                -> "SKU123456",
        "goodsDescription"         -> "Bananas",
        "countryOfOrigin"          -> "GB",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"
      val errorMessage = (contentAsJson(result) \ "message" \ "obj.comcode").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 400 when goodsDescription is missing" in {
      val invalidJsonRequest = Json.obj(
        "actorId"                  -> "GB987654321098",
        "traderRef"                -> "SKU123456",
        "comcode"                  -> 123456,
        "countryOfOrigin"          -> "GB",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"
      val errorMessage = (contentAsJson(result) \ "message" \ "obj.goodsDescription").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 400 when countryOfOrigin is missing" in {
      val invalidJsonRequest = Json.obj(
        "actorId"                  -> "GB987654321098",
        "traderRef"                -> "SKU123456",
        "comcode"                  -> 123456,
        "goodsDescription"         -> "Bananas",
        "category"                 -> 2,
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"
      val errorMessage = (contentAsJson(result) \ "message" \ "obj.countryOfOrigin").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 400 when category is missing" in {
      val invalidJsonRequest = Json.obj(
        "actorId"                  -> "GB987654321098",
        "traderRef"                -> "SKU123456",
        "comcode"                  -> 123456,
        "goodsDescription"         -> "Bananas",
        "countryOfOrigin"          -> "GB",
        "comcodeEffectiveFromDate" -> "2023-01-01T00:00:00Z"
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"
      val errorMessage = (contentAsJson(result) \ "message" \ "obj.category").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 400 when comcodeEffectiveFromDate is missing" in {
      val invalidJsonRequest = Json.obj(
        "actorId"          -> "GB987654321098",
        "traderRef"        -> "SKU123456",
        "comcode"          -> 123456,
        "goodsDescription" -> "Bananas",
        "countryOfOrigin"  -> "GB",
        "category"         -> 2
      )

      val result = sut.createRecord(eoriNumber)(request.withBody(invalidJsonRequest))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] mustBe "INVALID JSON"
      val errorMessage = (contentAsJson(result) \ "message" \ "obj.comcodeEffectiveFromDate").as[String]
      errorMessage mustBe "error.path.missing"
    }

    "return 500 when the router service returns an error" in {
      val createRequest = createAPICreateRecordRequest()

      val expectedJson = Json.obj(
        "timestamp" -> timestamp,
        "code"      -> "INTERNAL_SERVER_ERROR",
        "message"   -> "Sorry, the service is unavailable. You'll be able to use the service later"
      )

      when(routerService.createRecord(any, any)(any))
        .thenReturn(EitherT.leftT(InternalServerError(expectedJson)))

      val result = sut.createRecord(eoriNumber)(request.withBody(Json.toJson(createRequest)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }
  }
}
