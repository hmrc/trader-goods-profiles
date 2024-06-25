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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{GetRecordsResponse, GoodsItemRecords, Pagination}
import uk.gov.hmrc.tradergoodsprofiles.models.{Assessment, Condition}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with GetRecordResponseSupport
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
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, routerService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(routerService.getRecord(any, any)(any))
      .thenReturn(Future.successful(Right(createGetRecordResponse(eoriNumber, recordId, timestamp))))
    when(routerService.getRecords(any, any, any, any)(any))
      .thenReturn(Future.successful(Right(createGetRecordsResponse(eoriNumber, recordId, timestamp))))
  }

  "getRecord" should {
    "get the record from router" in {
      val result = sut.getRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
      verify(routerService).getRecord(eqTo(eoriNumber), eqTo(recordId))(any)
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

        when(routerService.getRecord(any, any)(any))
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
      verify(routerService)
        .getRecords(eqTo(eoriNumber), eqTo(Some("2024-03-26T16:14:52Z")), eqTo(Some(1)), eqTo(Some(1)))(any)
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

        when(routerService.getRecords(any, any, any, any)(any))
          .thenReturn(Future.successful(Left(serviceError)))

        val result = sut.getRecords(eoriNumber, Some("2024-03-26T16:14:52Z"), Some(0), Some(0))(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }

    }
  }

  def createGetRecordsResponse(
    eori: String,
    recordId: String,
    timestamp: Instant
  ): GetRecordsResponse = {
    val condition        = Condition(
      Some("certificate"),
      Some("Y923"),
      Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
      Some("Excluded product")
    )
    val assessment       = Assessment(Some("a06846e9a5f61fa4ecf2c4e3b23631fc"), Some(1), Some(condition))
    val goodsItemRecords = GoodsItemRecords(
      eori,
      "GB123456789012",
      recordId,
      "SKU123456",
      "123456",
      "Not Requested",
      "Bananas",
      "GB",
      2,
      Some(Seq(assessment)),
      Some(13),
      Some("Kilograms"),
      timestamp,
      Some(timestamp),
      1,
      active = true,
      toReview = false,
      Some("Commodity code changed"),
      "IMMI declarable",
      "XIUKIM47699357400020231115081800",
      Some("RMS-GB-123456"),
      Some("6 S12345"),
      locked = false,
      timestamp,
      timestamp
    )

    val pagination = Pagination(
      1,
      0,
      1,
      None,
      None
    )

    GetRecordsResponse(Seq(goodsItemRecords), pagination)
  }
}
