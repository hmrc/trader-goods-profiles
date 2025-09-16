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

import org.mockito.Mockito.{reset, verify, when}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.GetRecordsRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, FakeUserAllowListAction}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with GetRecordResponseSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request             = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId            = UUID.randomUUID().toString
  private val correlationId       = "d677693e-9981-4ee3-8574-654981ebe606"
  private val timestamp           = Instant.parse("2024-01-12T12:12:12Z")
  private val uuidService         = mock[UuidService]
  private val getRecordsConnector = mock[GetRecordsRouterConnector]
  private val appConfig           = mock[AppConfig]
  private val sut                 = new GetRecordsController(
    new FakeSuccessAuthAction(),
    new FakeUserAllowListAction(),
    uuidService,
    getRecordsConnector,
    appConfig,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, getRecordsConnector)
    when(uuidService.uuid).thenReturn(correlationId)
    when(getRecordsConnector.get(any, any)(any))
      .thenReturn(Future.successful(Right(createGetRecordResponse(eoriNumber, recordId, timestamp))))
    when(getRecordsConnector.get(any, any, any, any)(any))
      .thenReturn(Future.successful(Right(createGetRecordsResponse(eoriNumber, recordId, timestamp))))
  }

  "getRecord" should {
    "get the record from router" in {
      val result = sut.getRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
      verify(getRecordsConnector).get(eqTo(eoriNumber), eqTo(recordId))(any)
    }

    "return an error" when {

      "routerService return an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"Internal Server Error"
        )

        val errorResponse =
          ErrorResponse.serverErrorResponse(
            uuidService.uuid,
            "Internal Server Error"
          )
        val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

        when(getRecordsConnector.get(any, any)(any))
          .thenReturn(Future.successful(Left(serviceError)))

        val result = sut.getRecord(eoriNumber, recordId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }

  "getRecords" should {
    "return 200 with multiple records" in {
      val result = sut.getRecords(eoriNumber, Some("2024-03-26T16:14:52Z"), Some(1), Some(1))(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createGetRecordsResponse(eoriNumber, recordId, timestamp))
      verify(getRecordsConnector)
        .get(eqTo(eoriNumber), eqTo(Some("2024-03-26T16:14:52Z")), eqTo(Some(1)), eqTo(Some(1)))(any)
    }
    

    "return an error" when {
      "routerService return an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"Internal Server Error"
        )

        val errorResponse =
          ErrorResponse.serverErrorResponse(
            uuidService.uuid,
            "Internal Server Error"
          )
        val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

        when(getRecordsConnector.get(any, any, any, any)(any))
          .thenReturn(Future.successful(Left(serviceError)))

        val result = sut.getRecords(eoriNumber, Some("2024-03-26T16:14:52Z"), Some(0), Some(0))(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }

    }
  }
}
