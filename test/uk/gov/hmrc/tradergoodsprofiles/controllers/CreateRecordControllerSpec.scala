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
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.CreateRecordRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.UpdateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CreateRecordControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with UpdateRecordRequestSupport
    with CreateOrUpdateRecordResponseSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request               = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId              = UUID.randomUUID().toString
  private val timestamp             = Instant.parse("2024-01-12T12:12:12Z")
  private val correlationId         = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService           = mock[UuidService]
  private val createRecordConnector = mock[CreateRecordRouterConnector]
  private val appConfig             = mock[AppConfig]
  private val sut                   = new CreateRecordController(
    new FakeSuccessAuthAction(),
    createRecordConnector,
    uuidService,
    appConfig,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, createRecordConnector)
    when(uuidService.uuid).thenReturn(correlationId)
    when(createRecordConnector.createRecord(any, any)(any))
      .thenReturn(Future.successful(Right(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))))
  }

  "createRecord" should {
    "return 201 when the record is successfully created" in {

      val result = sut.createRecord(eoriNumber)(request.withBody(createUpdateRecordRequestData))

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.toJson(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))
    }

    "return 500 when the router service returns an error" in {

      val expectedJson  = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Sorry, the service is unavailable. You'll be able to use the service later"
      )
      val errorResponse =
        ErrorResponse.serverErrorResponse(
          uuidService.uuid,
          "Sorry, the service is unavailable. You'll be able to use the service later"
        )
      val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

      when(createRecordConnector.createRecord(any, any)(any))
        .thenReturn(Future.successful(Left(serviceError)))

      val result = sut.createRecord(eoriNumber)(request.withBody(createUpdateRecordRequestData))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }
  }

  lazy val invalidCreateRecordRequestDataForAssessmentArray: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "assessments": [
             |        {
             |            "assessmentId": "abc123",
             |            "primaryCategory": 1,
             |            "condition": {
             |                "type": "abc123",
             |                "conditionId": "Y923",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        },
             |        {
             |            "assessmentId": "",
             |            "primaryCategory": "test",
             |            "condition": {
             |                "type": "",
             |                "conditionId": "",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        }
             |    ],
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)

}
