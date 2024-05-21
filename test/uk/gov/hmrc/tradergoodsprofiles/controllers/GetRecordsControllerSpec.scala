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
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, GetRecordResponseSupport, GetRecordsResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class GetRecordsControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with GetRecordResponseSupport
    with GetRecordsResponseSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request       = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId      = UUID.randomUUID().toString
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val timestamp     = Instant.parse("2024-01-12T12:12:12Z")
  private val uuidService   = mock[UuidService]
  private val routerService = mock[RouterService]
  private val sut           = new GetRecordsController(
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
    when(routerService.getRecord(any, any)(any))
      .thenReturn(EitherT.fromEither(Right(createGetRecordResponse(eoriNumber, recordId, timestamp))))
    when(routerService.getRecords(any, any, any, any)(any))
      .thenReturn(EitherT.fromEither(Right(createGetRecordsResponse(eoriNumber, recordId, timestamp))))
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
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Bad Request",
          "errors"        -> Seq(
            Json.obj(
              "code"    -> "025",
              "message" -> "The recordId has been provided in the wrong format"
            )
          )
        )
      }

      "recordId is null" in {
        val result = sut.getRecord(eoriNumber, null)(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Bad Request",
          "errors"        -> Seq(
            Json.obj(
              "code"    -> "025",
              "message" -> "The recordId has been provided in the wrong format"
            )
          )
        )
      }

      "recordId is empty" in {
        val result = sut.getRecord(eoriNumber, " ")(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "BAD_REQUEST",
          "message"       -> "Bad Request",
          "errors"        -> Seq(
            Json.obj(
              "code"    -> "025",
              "message" -> "The recordId has been provided in the wrong format"
            )
          )
        )
      }

      "routerService return an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"Internal Server Error"
        )

        when(routerService.getRecord(any, any)(any))
          .thenReturn(EitherT.fromEither(Left(InternalServerError(expectedJson))))

        val result = sut.getRecord(eoriNumber, recordId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }

  "getRecords" should {
    "return 200 records without pagination" in {
      val result = sut.getRecords(eoriNumber, None, Some(0), Some(0))(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createGetRecordsResponse(eoriNumber, recordId, timestamp))
    }

    "return 200 records with pagination" in {
      val result = sut.getRecords(eoriNumber, Some("2024-03-26T16:14:52Z"), Some(1), Some(1))(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(createGetRecordsResponse(eoriNumber, recordId, timestamp))
    }

    "get the record from router" in {
      val result = sut.getRecords(eoriNumber, Some(""), Some(0), Some(0))(request)

      status(result) mustBe OK
      verify(routerService).getRecords(eqTo(eoriNumber), eqTo(Some("")), eqTo(Some(0)), eqTo(Some(0)))(any)
    }

    "return an error" when {
      "routerService return an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"Internal Server Error"
        )

        when(routerService.getRecords(any, any, any, any)(any))
          .thenReturn(EitherT.fromEither(Left(InternalServerError(expectedJson))))

        val result = sut.getRecords(eoriNumber, Some(""), Some(0), Some(0))(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }
}
