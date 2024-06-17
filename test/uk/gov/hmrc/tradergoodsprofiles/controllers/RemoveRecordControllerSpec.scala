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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request = FakeRequest()
    .withHeaders(
      "Accept"       -> "application/vnd.hmrc.1.0+json",
      "Content-Type" -> "application/json",
      "X-Client-ID"  -> "some client ID"
    )

  val requestHeaders: Seq[(String, String)] = Seq(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json"
  )
  private val recordId                      = UUID.randomUUID().toString
  private val actorId                       = "GB987654321098"
  private val invalidActorId                = "INVALID_ACTOR_ID"
  private val correlationId                 = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService                   = mock[UuidService]
  private val routerService                 = mock[RouterService]
  private val sut                           = new RemoveRecordController(
    new FakeSuccessAuthAction(),
    new ValidateHeaderAction(uuidService),
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, routerService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(routerService.removeRecord(any, any, any)(any))
      .thenReturn(Future.successful(Right(OK)))
  }

  "removeRecord" should {
    "return 204" in {
      val result = sut.removeRecord(eoriNumber, recordId, Some(actorId))(request)
      status(result) mustBe NO_CONTENT
    }

    "remove the record from router" in {
      val result = sut.removeRecord(eoriNumber, recordId, Some(actorId))(request)

      status(result) mustBe NO_CONTENT
      verify(routerService).removeRecord(eqTo(eoriNumber), eqTo(recordId), eqTo(actorId))(any)
    }

    "return an error" when {

      "recordId is not a UUID" in {
        val result = sut.removeRecord(eoriNumber, "1234-abc", Some(actorId))(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe createInvalidRequestParameterExpectedJson
      }

      "recordId is null" in {
        val result = sut.removeRecord(eoriNumber, null, Some(actorId))(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe createInvalidRequestParameterExpectedJson
      }

      "recordId is empty" in {
        val result = sut.removeRecord(eoriNumber, " ", Some(actorId))(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe createInvalidRequestParameterExpectedJson
      }

      "actorId is invalid" in {
        val result = sut.removeRecord(eoriNumber, recordId, Some(invalidActorId))(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe createInvalidActorIdExpectedJson
      }

      "actorId is empty" in {
        val result = sut.removeRecord(eoriNumber, recordId, Some(""))(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe createInvalidActorIdExpectedJson
      }

      "actorId is missing" in {
        val result = sut.removeRecord(eoriNumber, recordId, None)(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe createInvalidActorIdExpectedJson
      }

      "routerService returns an error" in {
      "routerService return an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> "d677693e-9981-4ee3-8574-654981ebe606",
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"internal server error"
        )

        val errorResponse =
          ErrorResponse.serverErrorResponse(uuidService.uuid, "internal server error")
        val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

        when(routerService.removeRecord(any, any, any)(any))
          .thenReturn(Future.successful(Left(serviceError)))

        val result = sut.removeRecord(eoriNumber, recordId, Some(actorId))(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }

  private def createInvalidRequestParameterExpectedJson: JsObject =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Seq(
        Json.obj(
          "code"        -> "INVALID_REQUEST_PARAMETER",
          "message"     -> "The recordId has been provided in the wrong format",
          "errorNumber" -> 25
        )
      )
    )

  private def createInvalidActorIdExpectedJson: JsObject =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Seq(
        Json.obj(
          "code"        -> "INVALID_REQUEST_PARAMETER",
          "message"     -> ApplicationConstants.InvalidActorIdQueryParameter,
          "errorNumber" -> 8
        )
      )
    )
}
