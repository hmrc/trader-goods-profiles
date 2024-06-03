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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.requests.UpdateProfileRequest
import uk.gov.hmrc.tradergoodsprofiles.models.responses.UpdateProfileResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import scala.concurrent.ExecutionContext

class MaintainProfileControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val url       = s"http://localhost:$port/$eoriNumber"
  private val routerUrl = s"/trader-goods-profiles-router/maintainprofile/v1/$eoriNumber"

  private val updateProfileRequest = UpdateProfileRequest(
    actorId = "GB987654321098",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345")
  )

  private val updateProfileResponse = UpdateProfileResponse(
    eoriNumber,
    "GB987654321098",
    "XIUKIM47699357400020231115081800",
    "RMS-GB-123456",
    "6 S12345"
  )

  private val requestBody      = Json.toJson(updateProfileRequest)
  private val expectedResponse = Json.toJson(updateProfileResponse)

  override lazy val app: Application = {
    wireMock.start()
    configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServices)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[UuidService].to(uuidService),
        bind[HttpClientV2].to(httpClientV2)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)
    stubRouterRequest(OK, expectedResponse.toString())
    when(uuidService.uuid).thenReturn(correlationId)

  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "MaintainProfileController" should {
    "return 200 OK when the profile update is successful" in {
      withAuthorizedTrader()

      val result = updateProfileAndWait()

      println(s"Response: ${result.status}, Body: ${result.body}")

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        verify(
          putRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "return 400 Bad Request when the JSON body is invalid" in {
      withAuthorizedTrader()
      val invalidJson = Json.parse("""{"invalid": "json"}""")

      val result = updateProfileAndWait(invalidJson)

      result.status mustBe BAD_REQUEST
      // TODO: Assert actual validation error response and include more test cases for invalid or missing fields
    }

    "return 401 Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = updateProfileAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateProfileExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return 500 Internal Server Error when the service layer fails" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = updateProfileAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe updateProfileExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber with error: runtime exception"
      )
    }
  }

  private def updateProfileAndWait(requestBody: JsValue = requestBody) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID"  -> "clientId",
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .put(requestBody)
    )

  private def stubRouterRequest(status: Int, responseBody: String) =
    wireMock.stubFor(
      put(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )

  private def updateProfileExpectedJson(code: String, message: String): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> code,
      "message"       -> message
    )
}
