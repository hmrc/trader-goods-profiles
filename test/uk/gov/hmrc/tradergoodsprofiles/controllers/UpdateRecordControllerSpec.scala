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
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.UpdateRecordRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.UpdateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, FakeUserAllowListAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

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

  private val recordId      = UUID.randomUUID().toString
  private val timestamp     = Instant.parse("2024-01-12T12:12:12Z")
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService   = mock[UuidService]
  private val connector     = mock[UpdateRecordRouterConnector]
  private val appConfig     = mock[AppConfig]
  private val sut           = new UpdateRecordController(
    new FakeSuccessAuthAction(),
    new FakeUserAllowListAction(),
    connector,
    appConfig,
    uuidService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, connector)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "patchRecord" should {
    val request = createFakeRequestWithHeaders(
      "Accept"       -> "application/vnd.hmrc.1.0+json",
      "Content-Type" -> "application/json",
      "X-Client-ID"  -> "some client ID"
    )

    "return 200 when the record is successfully updated" in {
      when(connector.patch(any, any, any)(any))
        .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))
      val result = sut.patchRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))
    }

    /*
  ToDO: remove this test after drop1.1 - TGP-1903

  The client ID does not need to be checked anymore as EIS has removed it
  from the header
     */
    "not validate client ID is isClientIdOptional is true" in {
      when(connector.patch(any, any, any)(any))
        .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))
      when(appConfig.isClientIdOptional).thenReturn(true)

      val request = createFakeRequestWithHeaders(
        "Accept"       -> "application/vnd.hmrc.1.0+json",
        "Content-Type" -> "application/json"
      )
      val result  = sut.patchRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
    }

    "return 500 when the router service returns an error" in {
      when(connector.patch(any, any, any)(any))
        .thenReturn(Future.successful(Left(createRouterErrorResponse)))

      val result = sut.patchRecord(eoriNumber, recordId)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> uuidService.uuid,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Sorry, the service is unavailable. You'll be able to use the service later"
      )
    }

    "return a 400" when {
      "Content-Type header is invalid" in {
        when(connector.patch(any, any, any)(any))
          .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))

        val request = createFakeRequestWithHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        val result  = sut.patchRecord(eoriNumber, recordId)(request)

        status(result) mustBe BAD_REQUEST
      }

      "Accept header is invalid" in {
        when(connector.patch(any, any, any)(any))
          .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))

        val request = createFakeRequestWithHeaders("Content-Type" -> "application/json")
        val result  = sut.patchRecord(eoriNumber, recordId)(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "updateRecord" should {

    val putRequest = createFakeRequestWithHeaders(
      "Accept"       -> "application/vnd.hmrc.1.0+json",
      "Content-Type" -> "application/json"
    )

    "return 200 when the record is successfully updated" in {
      when(connector.put(any, any, any)(any))
        .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))

      val result = sut.updateRecord(eoriNumber, recordId)(putRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))

      withClue("should sent a request") {
        val captor = ArgCaptor[Request[JsValue]]
        verify(connector).put(eqTo(eoriNumber), eqTo(recordId), captor.capture)(any)

        captor.value.body mustBe createUpdateRecordRequestData
      }
    }

    "return 500 when the router service returns an error" in {
      when(connector.put(any, any, any)(any))
        .thenReturn(Future.successful(Left(createRouterErrorResponse)))

      val result = sut.updateRecord(eoriNumber, recordId)(putRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> uuidService.uuid,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Sorry, the service is unavailable. You'll be able to use the service later"
      )
    }

    "return a 400" when {
      "Content-Type header is invalid" in {
        when(connector.put(any, any, any)(any))
          .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))

        val request = createFakeRequestWithHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        val result  = sut.updateRecord(eoriNumber, recordId)(request)

        status(result) mustBe BAD_REQUEST
      }

      "Accept header is invalid" in {
        when(connector.put(any, any, any)(any))
          .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))

        val request = createFakeRequestWithHeaders("Content-Type" -> "application/json")
        val result  = sut.updateRecord(eoriNumber, recordId)(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  private def createFakeRequestWithHeaders(headers: (String, String)*) =
    FakeRequest()
      .withHeaders(headers: _*)
      .withBody(createUpdateRecordRequestData)

  private def createRouterErrorResponse: ServiceError = {
    val errorResponse = ErrorResponse.serverErrorResponse(
      correlationId,
      "Sorry, the service is unavailable. You'll be able to use the service later"
    )

    ServiceError(INTERNAL_SERVER_ERROR, errorResponse)
  }
}
