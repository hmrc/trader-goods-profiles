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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.services.{DateTimeService, RouterService}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class GetRecordsControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with GetRecordResponseSupport
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
  private val sut             = new GetRecordsController(
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
    when(routerService.getRecord(any, any)(any))
      .thenReturn(EitherT.fromEither(Right(createGetRecordResponse(eoriNumber, recordId, timestamp))))
  }

  "getRecord" should {
    "return 200" in {
      val result = sut.getRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
    }

    "get the record from router" in {
      val result = sut.getRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
      verify(routerService).getRecord(eqTo(eoriNumber), eqTo(recordId))(any)
    }

    "return an error" when {

      "recordId is not a UUID" in {
        val result = sut.getRecord(eoriNumber, "1234-abc")(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "timestamp" -> timestamp,
          "code"      -> "INVALID_RECORD_ID_PARAMETER",
          "message"   -> "Invalid record ID supplied for eori number provided"
        )
      }

      "recordId is null" in {
        val result = sut.getRecord(eoriNumber, null)(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "timestamp" -> timestamp,
          "code"      -> "INVALID_RECORD_ID_PARAMETER",
          "message"   -> "Invalid record ID supplied for eori number provided"
        )
      }

      "recordId is empty" in {
        val result = sut.getRecord(eoriNumber, " ")(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "timestamp" -> timestamp,
          "code"      -> "INVALID_RECORD_ID_PARAMETER",
          "message"   -> "Invalid record ID supplied for eori number provided"
        )
      }

      "routerService return an error" in {
        val expectedJson = Json.obj(
          "code"    -> "INTERNAL_SERVER_ERROR",
          "message" -> s"internal server error"
        )

        when(routerService.getRecord(any, any)(any))
          .thenReturn(EitherT.fromEither(Left(InternalServerError(expectedJson))))

        val result = sut.getRecord(eoriNumber, recordId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }
}
