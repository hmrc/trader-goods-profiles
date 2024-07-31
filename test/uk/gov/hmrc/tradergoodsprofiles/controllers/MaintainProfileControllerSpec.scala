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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.MaintainProfileRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.responses.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

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
  private val connector     = mock[MaintainProfileRouterConnector]
  private val appConfig     = mock[AppConfig]

  def updateProfileRequestData(): JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB987654321098",
             |    "ukimsNumber": "XIUKIM47699357400020231115081800",
             |    "nirmsNumber": "RMS-GB-123456",
             |    "niphlNumber": "6 S12345"
             |
             |}
             |""".stripMargin)

  def updateProfileJson(): Request[JsValue]  =
    FakeRequest().withBody(updateProfileRequestData())
  val updateProfileRequest: Request[JsValue] = updateProfileJson()

  private val updateProfileResponse = MaintainProfileResponse(
    eori,
    "GB987654321098",
    Some("XIUKIM47699357400020231115081800"),
    Some("RMS-GB-123456"),
    Some("6 S12345")
  )

  private val sut = new MaintainProfileController(
    new FakeSuccessAuthAction(),
    connector,
    uuidService,
    appConfig,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, connector, appConfig)
    when(uuidService.uuid).thenReturn(correlationId)
    when(connector.put(mockEq(eori), any[Request[JsValue]])(any()))
      .thenReturn(Future.successful(Right(updateProfileResponse)))
    when(appConfig.isDrop1_1_enabled).thenReturn(false)
  }

  "MaintainProfileController" should {

    // TODO: Create a single test - Ticket-2014
    "return 200 OK when the profile update is successful" in {
      val request: FakeRequest[JsValue] = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(updateProfileRequest.body)

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(updateProfileResponse)
    }
    // TODO: Create a single test - Ticket-2014
    "return 200 OK without validating x-client-id when isDrop1_1_enabled is true" in {
      val requestWithoutClientId = FakeRequest()
        .withHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .withBody(updateProfileRequest.body)

      when(appConfig.isDrop1_1_enabled).thenReturn(true)

      val result = sut.updateProfile(eori)(requestWithoutClientId)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(updateProfileResponse)
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

      when(connector.put(any, any)(any))
        .thenReturn(Future.successful(Left(serviceError)))

      val request = FakeRequest()
        .withHeaders(requestHeaders: _*)
        .withBody(updateProfileRequest.body)

      val result = sut.updateProfile(eori)(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }

  }

}
